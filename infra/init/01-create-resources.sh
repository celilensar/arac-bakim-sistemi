#!/bin/bash
# Bu script LocalStack konteynerinin İÇİNDE, LocalStack hazır olduğunda otomatik çalışır.
# Görevi: Faz 1 için gereken AWS kaynaklarını oluşturmak — bir SQS kuyruğu ve bir DynamoDB tablosu.
# "awslocal", LocalStack'in içinde gelen, AWS CLI'ın --endpoint-url=http://localhost:4566 ayarlı kısayoludur.

set -e   # herhangi bir komut hata verirse script dursun

echo "### [init] Telemetri SQS kuyrugu olusturuluyor..."
awslocal sqs create-queue --queue-name telemetry-queue

echo "### [init] Telemetri DynamoDB tablosu olusturuluyor..."
awslocal dynamodb create-table \
  --table-name telemetry \
  --attribute-definitions \
      AttributeName=vehicleId,AttributeType=S \
      AttributeName=timestamp,AttributeType=S \
  --key-schema \
      AttributeName=vehicleId,KeyType=HASH \
      AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST

echo "### [init] Kaynaklar hazir."
