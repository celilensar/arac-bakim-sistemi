# Araç Bakım Uyarı Sistemi

Simüle edilmiş araç sensör verilerini (motor sıcaklığı, yağ ömrü, lastik basıncı,
akü voltajı, kilometre vb.) işleyerek bakım uyarıları üreten, olay-güdümlü bir
mikroservis sistemi. Amaç AWS serverless mimari, Spring Boot mikroservisler ve
mesaj kuyrukları (SQS/SNS) konularını uçtan uca pratik etmek.

Geliştirme **lokal**de yapılır: AWS servisleri [LocalStack](https://localstack.cloud)
ile Docker üzerinde simüle edilir (ücret yok).

## Mimari (Faz 1 — çekirdek akış)

```
[Sensör Simülatörü] --> [SQS] --> [Ingestion] --> [DynamoDB] <-- [Query API]
   Spring Boot         LocalStack  Spring Boot    LocalStack    Spring Boot
```

## Servisler

| Klasör | Açıklama | Durum |
|--------|----------|-------|
| `infra/` | LocalStack + kaynak oluşturma scriptleri | ✅ |
| `sensor-simulator/` | Periyodik telemetri üretip SQS'e basar | ✅ |
| `ingestion-service/` | SQS'ten okur, doğrular, DynamoDB'ye yazar | ⬜ |
| `query-api/` | DynamoDB'den okuyup REST ile sunar | ⬜ |

## Çalıştırma

```bash
# 1) Lokal AWS'i (LocalStack) ayağa kaldır
docker compose up -d

# 2) Simülatörü çalıştır
cd sensor-simulator
./mvnw spring-boot:run
```

## Gereksinimler

- JDK 21
- Docker (Docker Desktop)
- (opsiyonel) AWS CLI — `awslocal` ile kaynakları incelemek için

## Yol haritası

- **Faz 1** — Çekirdek akış: Simülatör → SQS → Ingestion → DynamoDB → API
- **Faz 2** — Mesajlaşma + kurallar: SNS fan-out, kural motoru, uyarı kuyruğu, DLQ
- **Faz 3** — Bildirim + frontend: SNS e-posta, React dashboard
- **Faz 4** — Cila: kimlik doğrulama, IaC (Terraform/CDK), CI/CD
