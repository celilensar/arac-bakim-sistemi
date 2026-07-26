package com.aracbakim.rules_engine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * rules-queue'yu tuketir; her telemetriye kurallari uygular; tetiklenen uyarilari
 * hem alerts tablosuna yazar hem alerts-queue'ya basar; sonra mesaji siler.
 *
 * Faz 1 consumer deseninin aynisi (receive -> isle -> delete), uzerine is mantigi.
 */
@Component
public class RulesConsumer {

    private static final Logger log = LoggerFactory.getLogger(RulesConsumer.class);

    private final SqsClient sqsClient;
    private final DynamoDbTable<Alert> alertsTable;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sqs.rules-queue-name}")
    private String rulesQueueName;

    @Value("${app.sqs.alerts-queue-name}")
    private String alertsQueueName;

    private String rulesQueueUrl;
    private String alertsQueueUrl;

    public RulesConsumer(SqsClient sqsClient, DynamoDbTable<Alert> alertsTable, RuleEngine ruleEngine) {
        this.sqsClient = sqsClient;
        this.alertsTable = alertsTable;
        this.ruleEngine = ruleEngine;
    }

    @Scheduled(fixedDelayString = "${app.poll.interval-ms}")
    public void poll() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(b -> b
                .queueUrl(url(rulesQueueName))
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5));

        for (Message message : response.messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            // 1) Gelen telemetriyi cozumle
            Telemetry t = objectMapper.readValue(message.body(), Telemetry.class);

            // 2) Kurallari uygula
            List<Alert> alerts = ruleEngine.evaluate(t);

            // 3) Her uyari icin: alerts tablosuna yaz + alerts-queue'ya bas
            for (Alert alert : alerts) {
                alertsTable.putItem(alert);
                // JSON'u lambda'dan ONCE uret (lambda checked exception firlatamaz)
                String body = objectMapper.writeValueAsString(alert);
                sqsClient.sendMessage(s -> s
                        .queueUrl(url(alertsQueueName))
                        .messageBody(body));
                log.warn("UYARI [{}] {} -> {} ({}={})",
                        alert.getSeverity(), alert.getVehicleId(), alert.getMessage(),
                        alert.getRule(), alert.getValue());
            }

            if (alerts.isEmpty()) {
                log.info("OK {} - kural tetiklenmedi", t.getVehicleId());
            }

            // 4) Islendi -> mesaji sil
            sqsClient.deleteMessage(d -> d
                    .queueUrl(url(rulesQueueName))
                    .receiptHandle(message.receiptHandle()));

        } catch (Exception e) {
            // Silmiyoruz: mesaj kuyrukta kalir, tekrar denenir; 3 kez basarisiz olursa DLQ'ya gider.
            log.error("Kural islenemedi, kuyrukta birakildi: {}", message.messageId(), e);
        }
    }

    // Kuyruk adindan URL cozer ve saklar (rules-queue ve alerts-queue icin ayni yardimci).
    private String url(String queueName) {
        if (queueName.equals(rulesQueueName)) {
            if (rulesQueueUrl == null) {
                rulesQueueUrl = sqsClient.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
            }
            return rulesQueueUrl;
        } else {
            if (alertsQueueUrl == null) {
                alertsQueueUrl = sqsClient.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
            }
            return alertsQueueUrl;
        }
    }
}
