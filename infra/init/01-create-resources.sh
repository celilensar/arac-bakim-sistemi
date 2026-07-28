#!/bin/bash
# LocalStack hazir oldugunda otomatik calisir; tum AWS kaynaklarini olusturur.
# "awslocal" = LocalStack icinde gelen, otomatik localhost:4566'ya baglanan AWS CLI.

set -e   # herhangi bir komut hata verirse dur

echo "### [init] ===== FAZ 1 kaynaklari ====="

echo "### [init] telemetry-queue (SQS)"
awslocal sqs create-queue --queue-name telemetry-queue

echo "### [init] telemetry (DynamoDB)"
awslocal dynamodb create-table \
  --table-name telemetry \
  --attribute-definitions \
      AttributeName=vehicleId,AttributeType=S \
      AttributeName=timestamp,AttributeType=S \
  --key-schema \
      AttributeName=vehicleId,KeyType=HASH \
      AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST


echo "### [init] ===== FAZ 2 kaynaklari (SNS fan-out + kurallar) ====="

# Yardimci: bir kuyrugun ARN'ini dondurur
queue_arn() {
  awslocal sqs get-queue-attributes \
    --queue-url "$(awslocal sqs get-queue-url --queue-name "$1" --query QueueUrl --output text)" \
    --attribute-names QueueArn --query 'Attributes.QueueArn' --output text
}

# Yardimci: redrive policy'yi TAM JSON formatinda dogru kacisla uretir.
# (--attributes Key=Value kisayolu virgulde bolundugu icin JSON degerlerde bozulur;
#  o yuzden {"RedrivePolicy":"<escaped-json>"} seklinde tam JSON veriyoruz.)
redrive_attrs() {
  printf '{"RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"%s\\",\\"maxReceiveCount\\":\\"3\\"}"}' "$1"
}

# 1) SNS topic: telemetri olaylarinin yayinlandigi yer
TOPIC_ARN=$(awslocal sns create-topic --name telemetry-events --query 'TopicArn' --output text)
echo "### [init] SNS topic: $TOPIC_ARN"

# 2) Kural motoru kuyrugunun DLQ'su (once DLQ olsun ki redrive ona isaret edebilsin)
awslocal sqs create-queue --queue-name rules-dlq
RULES_DLQ_ARN=$(queue_arn rules-dlq)

# 3) rules-queue: redrive policy ile -> 3 basarisiz denemeden sonra mesaj DLQ'ya gider
awslocal sqs create-queue --queue-name rules-queue --attributes "$(redrive_attrs "$RULES_DLQ_ARN")"
RULES_QUEUE_ARN=$(queue_arn rules-queue)

# 4) rules-queue'yu SNS topic'e abone et.
#    RawMessageDelivery=true -> SNS "zarfi" olmadan ham JSON gelir (kural motoru dogrudan okur)
echo "### [init] rules-queue -> telemetry-events aboneligi"
awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$RULES_QUEUE_ARN" \
  --attributes RawMessageDelivery=true

# 5) Uyari kuyrugu + DLQ (kural motorunun uretecegi uyarilar icin - sonraki adimda kullanilacak)
awslocal sqs create-queue --queue-name alerts-dlq
ALERTS_DLQ_ARN=$(queue_arn alerts-dlq)
awslocal sqs create-queue --queue-name alerts-queue --attributes "$(redrive_attrs "$ALERTS_DLQ_ARN")"

# 6) Uyarilari saklayacagimiz DynamoDB tablosu (dashboard icin)
echo "### [init] alerts (DynamoDB)"
awslocal dynamodb create-table \
  --table-name alerts \
  --attribute-definitions \
      AttributeName=vehicleId,AttributeType=S \
      AttributeName=alertId,AttributeType=S \
  --key-schema \
      AttributeName=vehicleId,KeyType=HASH \
      AttributeName=alertId,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST


echo "### [init] ===== FAZ 3 kaynaklari (bildirim) ====="

# 1) Bildirim topic'i: notification servisi buraya yayinlar
NOTIFY_TOPIC_ARN=$(awslocal sns create-topic --name alert-notifications --query 'TopicArn' --output text)
echo "### [init] SNS topic: $NOTIFY_TOPIC_ARN"

# 2) "Gelen kutusu" kuyrugu: gercek e-posta yerine bildirimleri burada gorup dogrulayacagiz
awslocal sqs create-queue --queue-name notifications-inbox
INBOX_ARN=$(queue_arn notifications-inbox)

# 3) notifications-inbox'i bildirim topic'ine abone et (raw delivery)
echo "### [init] notifications-inbox -> alert-notifications aboneligi"
awslocal sns subscribe \
  --topic-arn "$NOTIFY_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$INBOX_ARN" \
  --attributes RawMessageDelivery=true

# Not: Gercek AWS'te buraya bir e-posta aboneligi de eklenir:
#   awslocal sns subscribe --topic-arn "$NOTIFY_TOPIC_ARN" --protocol email --notification-endpoint ornek@mail.com
# LocalStack (ucretsiz) gercek e-posta gondermez; o yuzden inbox kuyrugu ile dogruluyoruz.


echo "### [init] ===== Dinamik esikler (thresholds) ====="

# Kural esikleri artik koda gomulu degil; bu tabloda VERI olarak duruyor.
# Kural motoru bunlari periyodik okur -> deger degistirince restart gerekmez.
awslocal dynamodb create-table \
  --table-name thresholds \
  --attribute-definitions AttributeName=ruleCode,AttributeType=S \
  --key-schema AttributeName=ruleCode,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# Yardimci: bir esik kuralini tabloya yazar.
# arg: ruleCode metric operator(GT/LT) limitValue severity message
put_threshold() {
  awslocal dynamodb put-item --table-name thresholds --item \
    "{\"ruleCode\":{\"S\":\"$1\"},\"metric\":{\"S\":\"$2\"},\"operator\":{\"S\":\"$3\"},\"limitValue\":{\"N\":\"$4\"},\"severity\":{\"S\":\"$5\"},\"message\":{\"S\":\"$6\"},\"enabled\":{\"BOOL\":true}}"
}

put_threshold ENGINE_OVERHEAT    engineTemp     GT 105 KRITIK "Motor sicakligi cok yuksek"
put_threshold OIL_LIFE_LOW       oilLife        LT 15  UYARI  "Yag omru dusuk, bakim gerekli"
put_threshold BATTERY_LOW        batteryVoltage LT 12  UYARI  "Aku voltaji dusuk"
put_threshold TIRE_PRESSURE_LOW  tirePressure   LT 30  BILGI  "Lastik basinci dusuk"

echo "### [init] Tum kaynaklar hazir."
