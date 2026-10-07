# Mimari ve Akış Diyagramları

Bu belge, Araç Bakım Uyarı Sistemi'nin mimarisini ve temel akışlarını görselleştirir.
(GitHub bu `mermaid` bloklarını otomatik çizer.)

---

## 1. Sistem Mimarisi — Genel Görünüm

Beş bağımsız Spring Boot servisi; SQS kuyrukları ve SNS fan-out ile **asenkron** haberleşir.

```mermaid
flowchart TD
    SIM["sensor-simulator<br/>(telemetri üretir)"]
    Q1(["SQS<br/>telemetry-queue"])
    ING["ingestion-service"]
    DDB[("DynamoDB<br/>telemetry · vehicle_state")]
    SNS1((("SNS<br/>telemetry-events")))
    Q2(["SQS<br/>rules-queue"])
    RUL["rules-engine<br/>(dinamik eşikler)"]
    Q3(["SQS<br/>alerts-queue"])
    NOT["notification-service<br/>(cooldown)"]
    SNS2((("SNS<br/>alert-notifications")))
    MAIL["E-posta"]
    Q4(["SQS<br/>dashboard-queue"])
    API["query-api<br/>REST + SSE"]
    DASH["React + Three.js<br/>3B Dashboard"]

    SIM --> Q1 --> ING
    ING -->|koşullu yazma| DDB
    ING --> SNS1 --> Q2 --> RUL
    RUL --> Q3 --> NOT
    NOT --> SNS2
    SNS2 --> MAIL
    SNS2 --> Q4 --> API
    API -->|okur| DDB
    API -->|SSE push| DASH
```

---

## 2. Telemetri Akışı (sequence)

Bir sensör okumasının sisteme girişten veritabanına yazılmasına kadarki yolu.

```mermaid
sequenceDiagram
    participant S as sensor-simulator
    participant Q as SQS telemetry-queue
    participant I as ingestion-service
    participant D as DynamoDB
    participant N as SNS telemetry-events

    S->>Q: telemetri JSON gönder
    I->>Q: long polling ile oku
    Q-->>I: mesaj(lar)
    I->>D: telemetry tablosuna yaz (geçmiş)
    I->>D: vehicle_state'e KOŞULLU yaz<br/>(#ts < :newTs)
    alt Gelen ölçüm daha yeni
        D-->>I: yazıldı
    else Geç gelen ESKİ ölçüm
        D-->>I: ConditionalCheckFailed (beklenen)
        Note over I,D: Güncel durum korunur<br/>(out-of-order koruması)
    end
    I->>N: olayı yayınla (fan-out)
    I->>Q: mesajı sil
```

---

## 3. Uyarı Üretimi ve Bildirim Akışı

Eşik aşımından kullanıcıya bildirime kadar.

```mermaid
sequenceDiagram
    participant R as rules-engine
    participant T as DynamoDB thresholds
    participant A as SQS alerts-queue
    participant N as notification-service
    participant C as Redis (cooldown)
    participant P as SNS alert-notifications

    R->>T: eşikleri periyodik oku (30 sn)
    Note over R,T: Eşik değişince restart YOK
    R->>R: metrik vs. eşik karşılaştır
    alt Eşik aşıldı
        R->>A: uyarı üret (alertId = ts#ruleCode)
        N->>A: uyarıyı tüket
        N->>N: seviye filtresi (KRİTİK/UYARI)
        N->>C: isInCooldown(arac#kural)?
        alt Cooldown'da
            C-->>N: true
            Note over N: Atlandı (alert fatigue önlendi)
        else Serbest
            C-->>N: false
            N->>P: bildirimi yayınla
            N->>C: markSent (TTL 120 sn)
        end
    else Eşik aşılmadı
        Note over R: Uyarı üretilmez
    end
```

---

## 4. Dayanıklılık — DLQ (Dead-Letter Queue) Akışı

İşlenemeyen "zehirli" mesaj ana akışı tıkamaz.

```mermaid
flowchart LR
    M["Mesaj"] --> Q(["Kuyruk"])
    Q --> C{"Tüketici<br/>işleyebildi mi?"}
    C -->|Evet| DEL["Mesajı sil"]
    C -->|Hayır| RT["Visibility timeout<br/>→ tekrar dene"]
    RT --> N{"Deneme<br/>sayısı > 3?"}
    N -->|Hayır| Q
    N -->|Evet| DLQ(["DLQ<br/>rules-dlq / alerts-dlq"])
    DLQ --> INS["İncelenir,<br/>ana akış tıkanmaz"]
```

---

## 5. Bulut Dağıtım Mimarisi (AWS)

İstemciden veritabanına kadar üretim yolu.

```mermaid
flowchart LR
    U["İstemci / Dashboard"]
    GW["API Gateway<br/>(HTTP API)"]
    COG["Cognito<br/>JWT Authorizer"]
    ALB["Application<br/>Load Balancer (HTTPS)"]
    ECS["ECS Fargate<br/>query-api konteyneri"]
    AWS[("DynamoDB · SQS")]
    ECR["ECR<br/>(image deposu)"]

    U -->|"Authorization: JWT"| GW
    GW -.->|doğrula| COG
    COG -.->|"token yok → 401"| U
    GW -->|"geçerli → 200"| ALB
    ALB --> ECS
    ECS --> AWS
    ECR -.->|image çeker| ECS
```

---

## 6. CI/CD Akışı (GitHub Actions + OIDC)

Anahtarsız kimlik ile otomatik dağıtım.

```mermaid
flowchart LR
    P["git push<br/>(main)"] --> GA["GitHub Actions"]
    GA -->|"OIDC token"| ROLE["IAM rolünü üstlen<br/>(uzun ömürlü anahtar YOK)"]
    ROLE --> B["Docker build"]
    B --> PUSH["ECR'ye push<br/>(latest + commit SHA)"]
    PUSH --> D{"ECS servisi<br/>çalışıyor mu?"}
    D -->|Evet| DEP["force-new-deployment<br/>(rolling update)"]
    D -->|Hayır| SKIP["Atla<br/>(pipeline yeşil kalır)"]
```

---

## 7. Cooldown — Strategy Pattern

Aynı arayüz, değiştirilebilir depo: bellek veya Redis.

```mermaid
classDiagram
    class CooldownTracker {
        <<interface>>
        +isInCooldown(key) boolean
        +markSent(key) void
    }
    class InMemoryCooldownTracker {
        -Map lastSentAt
        -Clock clock
    }
    class RedisCooldownTracker {
        -StringRedisTemplate redis
        -long cooldownSeconds
    }
    class NotificationConsumer {
        -CooldownTracker cooldown
    }
    CooldownTracker <|.. InMemoryCooldownTracker
    CooldownTracker <|.. RedisCooldownTracker
    NotificationConsumer --> CooldownTracker : kullanır
```

**Neden:** In-memory cooldown yalnızca tek instance'ta çalışır; yatay ölçeklemede her kopyanın
kendi hafızası olduğu için aynı uyarı birden çok kez gönderilir. Redis (TTL) ile durum
instance'lar arasında paylaşılır ve mükerrer bildirim önlenir.

---

## 8. Yerel Geliştirme ve Bulut Geçişi (12-Factor)

Aynı kod, farklı ortam.

```mermaid
flowchart TD
    CODE["Aynı kaynak kod"]
    CODE --> L["Yerel çalıştırma"]
    CODE --> C["Bulut çalıştırma"]
    L --> LS["LocalStack<br/>(endpoint = localhost:4566)<br/>sahte anahtarlar"]
    C --> RA["Gerçek AWS<br/>(endpoint boş)<br/>IAM rolü"]
    LS --> Z["Ücret yok"]
    RA --> Y["Üretim ortamı"]
```
