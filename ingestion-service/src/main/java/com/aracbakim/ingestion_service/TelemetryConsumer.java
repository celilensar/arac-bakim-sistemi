package com.aracbakim.ingestion_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * SQS kuyrugunu surekli yoklar; gelen her telemetriyi DynamoDB'ye yazar
 * ve basarili olursa mesaji kuyruktan siler.
 *
 * SQS tuketici mantiginin ozu:
 *   receive -> isle (DynamoDB'ye yaz) -> delete
 * Eger islerken hata olursa mesaji SILMEYIZ; gorunmezlik suresi (visibility
 * timeout) dolunca mesaj tekrar gorunur ve yeniden denenir. Boylece veri kaybolmaz.
 */
@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final DynamoDbTable<TelemetryItem> telemetryTable;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sqs.queue-name}")
    private String queueName;

    @Value("${app.sns.topic-name}")
    private String topicName;

    private String queueUrl;
    private String topicArn;

    // Spring, uc bean'i de constructor uzerinden enjekte eder.
    public TelemetryConsumer(SqsClient sqsClient, SnsClient snsClient,
                             DynamoDbTable<TelemetryItem> telemetryTable) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.telemetryTable = telemetryTable;
    }

    @Scheduled(fixedDelayString = "${app.poll.interval-ms}")
    public void poll() {
        // Bir seferde en fazla 10 mesaj al; kuyruk bossa 5 sn'ye kadar bekle (long polling).
        ReceiveMessageResponse response = sqsClient.receiveMessage(b -> b
                .queueUrl(resolveQueueUrl())
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5));

        for (Message message : response.messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            // 1) JSON metnini TelemetryItem nesnesine cevir
            TelemetryItem item = objectMapper.readValue(message.body(), TelemetryItem.class);

            // 2) DynamoDB'ye yaz (ayni vehicleId + timestamp varsa uzerine yazar = idempotent)
            telemetryTable.putItem(item);

            // 3) Ayni telemetriyi SNS topic'ine yayinla (fan-out).
            //    Topic'e abone olan rules-queue'ya mesaj otomatik kopyalanir.
            snsClient.publish(p -> p
                    .topicArn(resolveTopicArn())
                    .message(message.body()));

            // 4) Basariyla islendi -> mesaji kuyruktan sil
            sqsClient.deleteMessage(d -> d
                    .queueUrl(resolveQueueUrl())
                    .receiptHandle(message.receiptHandle()));

            log.info("Islendi + yayinlandi -> {} @ {} (motor={}C)",
                    item.getVehicleId(), item.getTimestamp(), item.getEngineTemp());
        } catch (Exception e) {
            // Silmiyoruz: mesaj kuyrukta kalir, visibility timeout sonrasi yeniden denenir.
            log.error("Mesaj islenemedi, kuyrukta birakildi: {}", message.messageId(), e);
        }
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            queueUrl = sqsClient.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
            log.info("Kuyruk URL cozuldu: {}", queueUrl);
        }
        return queueUrl;
    }

    // Topic ARN'ini adindan cozer. createTopic idempotent'tir: topic zaten varsa
    // yenisini yaratmaz, mevcut olanin ARN'ini dondurur. Boylece ARN'i koda gommeyiz.
    private String resolveTopicArn() {
        if (topicArn == null) {
            topicArn = snsClient.createTopic(b -> b.name(topicName)).topicArn();
            log.info("Topic ARN cozuldu: {}", topicArn);
        }
        return topicArn;
    }
}
