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

echo "### [init] Tum kaynaklar hazir."
