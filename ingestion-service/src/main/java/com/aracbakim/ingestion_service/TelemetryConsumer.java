package com.aracbakim.ingestion_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
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
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sqs.queue-name}")
    private String queueName;

    @Value("${app.sns.topic-name}")
    private String topicName;

    @Value("${app.dynamodb.state-table-name}")
    private String stateTableName;

    private String queueUrl;
    private String topicArn;

    // Spring, dort bean'i de constructor uzerinden enjekte eder.
    public TelemetryConsumer(SqsClient sqsClient, SnsClient snsClient,
                             DynamoDbTable<TelemetryItem> telemetryTable,
                             DynamoDbClient dynamoDbClient) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.telemetryTable = telemetryTable;
        this.dynamoDbClient = dynamoDbClient;
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

            // 2) Telemetri GECMISINE yaz (her olcum ayri satir; sira karissa da hepsi saklanir)
            telemetryTable.putItem(item);

            // 2b) GUNCEL DURUMU kosullu guncelle: sadece bu olcum kayitlidan daha YENIyse.
            //     Gec gelen eski bir olcum guncel durumu ezemez (out-of-order korumasi).
            updateCurrentState(item);

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

    /**
     * Aracin guncel durumunu vehicle_state tablosuna KOSULLU yazar.
     * Kosul: kayit yoksa VEYA gelen timestamp kayitlidan buyukse yaz.
     * ISO-8601 zaman damgasi metin olarak sozlukte dogru siralanir, bu yuzden
     * "#ts < :newTs" karsilastirmasi zaman karsilastirmasi gibi calisir.
     * Kosul saglanmazsa (gec gelen eski veri) DynamoDB yazmayi reddeder -> guncel durum korunur.
     */
    private void updateCurrentState(TelemetryItem t) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("vehicleId", AttributeValue.fromS(t.getVehicleId()));
        item.put("timestamp", AttributeValue.fromS(t.getTimestamp()));
        item.put("engineTemp", AttributeValue.fromN(String.valueOf(t.getEngineTemp())));
        item.put("oilLife", AttributeValue.fromN(String.valueOf(t.getOilLife())));
        item.put("tirePressure", AttributeValue.fromN(String.valueOf(t.getTirePressure())));
        item.put("batteryVoltage", AttributeValue.fromN(String.valueOf(t.getBatteryVoltage())));
        item.put("mileage", AttributeValue.fromN(String.valueOf(t.getMileage())));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(stateTableName)
                    .item(item)
                    .conditionExpression("attribute_not_exists(vehicleId) OR #ts < :newTs")
                    .expressionAttributeNames(Map.of("#ts", "timestamp")) // timestamp rezerve kelime
                    .expressionAttributeValues(Map.of(":newTs", AttributeValue.fromS(t.getTimestamp())))
                    .build());
        } catch (ConditionalCheckFailedException e) {
            // Beklenen durum: gelen olcum guncel durumdan eski -> guncel durum korundu.
            log.info("Eski olcum, guncel durum korundu -> {} @ {}", t.getVehicleId(), t.getTimestamp());
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
