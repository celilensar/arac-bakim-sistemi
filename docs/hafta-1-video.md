# Hafta 1 — Video Transkripti

> Köşeli parantezler `[EKRAN: ...]` o an ekranda ne göstereceğini söyler; gerisini sesli oku. Hedef süre ~3 dakika.

**[EKRAN: Kod editörü veya proje klasörü açık]**

"Merhaba hocam. Bu, araç bakım uyarı sistemi projemin birinci hafta ilerlemesi.

Kısaca projenin amacı şu: Araçlardan gelen sensör verilerini — motor sıcaklığı, yağ ömrü, lastik basıncı, akü voltajı gibi — işleyip otomatik bakım uyarıları üreten bir sistem kuruyorum. Şimdilik gerçek araç yok, veriyi ben simüle ediyorum. Asıl hedefim teknik: AWS bulut mimarisini, mikroservisleri ve mesaj kuyruklarıyla olay-güdümlü iletişimi uçtan uca öğrenmek.

Mimaride bilinçli bir karar verdim: **hibrit** ilerliyorum. Projenin belkemiği tamamen AWS serverless — Lambda, SQS, SNS, DynamoDB. Ama iş mantığı taşıyan çekirdek servisleri, sadece Lambda yerine Spring Boot mikroservis olarak da yazıyorum. Bunun iki sebebi var: birincisi, böylece hem serverless hem klasik mikroservis dünyasını aynı projede göstermiş oluyorum; ikincisi de açıkçası CV'm için — Spring Boot ve mikroservis, backend iş ilanlarında en çok istenen beceriler, bunları somut bir projeyle gösterebilmek benim için değerli. Ayrıca her parçayı doğru araca eşledim: yapıştırıcı, olay-güdümlü işler Lambda; sürekli çalışan, ağır iş mantığı olan servisler Spring Boot. Yani karar keyfi değil, bilinçli.

**[EKRAN: docker compose up -d komutunu çalıştır, sonra docker ps]**

Geliştirmeyi gerçek AWS'te değil, lokalde yapıyorum. Burada **LocalStack** kullanıyorum — AWS servislerini bilgisayarımda, Docker üzerinde simüle eden bir araç. Bunu seçmemin sebebi: ücretsiz, hızlı ve internet gerektirmiyor; öğrenirken hata yapmaktan çekinmiyorum. Gördüğünüz gibi LocalStack konteyneri ayakta ve 'healthy' durumda.

Konteyner açılırken bir başlangıç scripti otomatik çalışıyor ve iki AWS kaynağı oluşturuyor: telemetri için bir **SQS kuyruğu** ve verileri saklamak için bir **DynamoDB tablosu**.

**[EKRAN: sensor-simulator'ı çalıştır — .\mvnw.cmd spring-boot:run]**

Şimdi birinci servisimi çalıştırıyorum: sensör simülatörü. Bu bir Spring Boot uygulaması ve her beş saniyede bir, üç araç için rastgele telemetri üretiyor.

**[EKRAN: konsolda 'Gonderildi -> {...}' satırları aksın]**

İşte burada görüyorsunuz — her araç için üretilen veri JSON'a çevrilip SQS kuyruğuna gönderiliyor.

Burada en önemli kavram **decoupling**, yani servisleri birbirinden ayırmak. Simülatör veriyi doğrudan başka bir servise göndermiyor; sadece kuyruğa bırakıp işine devam ediyor. Veriyi okuyacak servis çökse veya yavaş olsa bile mesajlar kuyrukta güvenle bekler, kaybolmaz. Mesaj kuyruğu kullanmamın asıl sebebi bu.

**[EKRAN: awslocal sqs get-queue-attributes ... ApproximateNumberOfMessages komutu]**

Bunu kanıtlamak için kuyruğa bakıyorum. Henüz veriyi okuyan bir servis yazmadım, o yüzden mesajlar birikmeli — ve görüyorsunuz, kuyrukta kırk beşin üzerinde mesaj bekliyor. Yani üretici tarafı çalışıyor.

**[EKRAN: GitHub repo sayfası + commit geçmişi]**

Son olarak, kodu GitHub'da düzenli ve anlamlı commit'lerle tutuyorum. Her adımı ayrı bir commit olarak görebilirsiniz: önce proje iskeleti, sonra altyapı, sonra simülatör.

Özetle bu hafta: mimariyi kurdum, lokal AWS ortamını ayağa kaldırdım ve veri üretip kuyruğa basan ilk servisi çalıştırdım. Gelecek hafta kuyruktaki bu veriyi okuyup veritabanına yazan servisi ekleyip akışı uçtan uca tamamlayacağım. Teşekkürler hocam."
