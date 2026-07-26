# Hafta 3 — Video Transkripti

> `[EKRAN: ...]` o an ne göstereceğini söyler; gerisini sesli oku. Hedef süre ~3-4 dakika.
> Ön hazırlık: LocalStack + üç servis (simülatör, ingestion, rules-engine) çalışıyor olsun.

**[EKRAN: Kod editörü / proje yapısı]**

"Merhaba hocam. Araç bakım uyarı sistemi projesinin üçüncü hafta ilerlemesi.

Önceki haftalarda sistem veriyi bir yerden alıp başka yere taşıyordu: simülatör telemetri üretiyor, kuyruğa gidiyor, veritabanına yazılıyordu. Bu hafta sistem ilk kez **karar vermeye** başladı — yani gelen veriye bakıp bakım uyarısı üretiyor. Bunu yaparken iki önemli konuyu uyguladım: SNS ile mesaj dağıtımı ve bir kural motoru.

**[EKRAN: ingestion-service konsolu — 'Islendi + yayinlandi' satırları]**

İlk olarak mesajlaşmayı büyüttüm. Önceki haftaki SQS kuyruğu tek bir tüketiciye veri verir. Bu hafta **SNS** ekledim. SNS bir yayın-abone sistemidir: bir olayı yayınlarsın, ona abone olan herkese kopyalanır. Buna fan-out denir. Şimdi ingestion servisi telemetriyi hem veritabanına yazıyor, hem de SNS topic'ine yayınlıyor. Böylece aynı veri birden çok yere gidebiliyor — ve yarın yeni bir tüketici eklemek istersem tek satır abonelik yeterli, üretici kodu hiç değişmiyor.

**[EKRAN: rules-engine konsolu — UYARI [KRITIK] / [UYARI] satırları akıyor]**

SNS'e abone olan bir kural kuyruğu var; onu da yeni yazdığım **kural motoru** tüketiyor. Kural motoru gelen her telemetriye eşik kurallarını uyguluyor: motor sıcaklığı yüz beş dereceyi geçerse kritik uyarı, yağ ömrü yüzde on beşin altındaysa uyarı, akü voltajı düşükse uyarı gibi. Görüyorsunuz, gerçek zamanlı olarak bakım uyarıları üretiliyor. Kural tetiklenmeyen sağlıklı okumalar için de 'OK' yazıyor.

**[EKRAN: awslocal dynamodb scan --table-name alerts --select COUNT, sonra birkaç kayıt]**

Üretilen her uyarı iki yere gidiyor: incelenebilmesi için bir uyarı kuyruğuna basılıyor, ve dashboard'da listelenebilmesi için ayrı bir 'alerts' veritabanı tablosuna yazılıyor. Burada tabloda biriken uyarıları görüyorsunuz — her birinde araç, kural, önem derecesi ve ölçülen değer var.

**[EKRAN: rules-dlq izleme döngüsü + rules-engine'de 3 hata denemesi]**

Son olarak dayanıklılık tarafını gösterdim. Sisteme bilinçli olarak bozuk bir mesaj gönderdim. Kural motoru bunu işleyemedi, ama önemli olan: sistem çökmedi, normal telemetri akmaya devam etti. Bozuk mesaj üç kez denendikten sonra otomatik olarak bir 'ölü mektup kuyruğuna' — dead letter queue'ya — taşındı. Yani hatalı mesaj kaybolmuyor, kenara alınıp inceleme için saklanıyor. Bu, gerçek üretim sistemlerinde beklenen bir davranış.

**[EKRAN: GitHub commit geçmişi]**

Tüm bu adımları düzenli commit'lerle GitHub'da tutmaya devam ediyorum.

Özetle bu hafta sistem veri taşımaktan karar vermeye geçti: SNS ile olay dağıtımı, eşik tabanlı bir kural motoru, uyarı üretimi ve dead letter queue ile hata dayanıklılığı. Gelecek hafta bu uyarıları bir bildirim servisiyle dışarı taşımayı ve bir dashboard ile görselleştirmeyi planlıyorum. Teşekkürler hocam."
