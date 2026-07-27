package com.aracbakim.notification_service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * alerts-queue'yu tuketir; onemli uyarilari insan-okunur bir bildirime cevirip
 * alert-notifications SNS topic'ine yayinlar. Topic'e abone olanlar (gercek AWS'te
 * e-posta/SMS, bizde notifications-inbox kuyrugu) bildirimi alir.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sqs.alerts-queue-name}")
    private String alertsQueueName;

    @Value("${app.sns.notify-topic-name}")
    private String notifyTopicName;

    @Value("${app.notify.severities}")
    private String severitiesCsv;

    private String alertsQueueUrl;
    private String notifyTopicArn;
    private Set<String> allowedSeverities;

    public NotificationConsumer(SqsClient sqsClient, SnsClient snsClient) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
    }

    @Scheduled(fixedDelayString = "${app.poll.interval-ms}")
    public void poll() {
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
            Alert alert = objectMapper.readValue(message.body(), Alert.class);

            // Filtre: sadece izin verilen seviyeler bildirim olur (BILGI atlanir)
            if (severities().contains(alert.getSeverity())) {
                String subject = "Bakim Uyarisi: " + alert.getVehicleId();
                String body = format(alert);

                snsClient.publish(p -> p
                        .topicArn(resolveTopicArn())
                        .subject(subject)
                        .message(body));

                log.info("BILDIRIM GONDERILDI -> {}", body);
            } else {
                log.info("Atlandi (dusuk oncelik: {}) -> {}", alert.getSeverity(), alert.getVehicleId());
            }

            // Islendi (gonderildi ya da bilincli atlandi) -> mesaji sil
            sqsClient.deleteMessage(d -> d
                    .queueUrl(resolveQueueUrl())
                    .receiptHandle(message.receiptHandle()));

        } catch (Exception e) {
            log.error("Bildirim islenemedi, kuyrukta birakildi: {}", message.messageId(), e);
        }
    }

    private String format(Alert a) {
        String icon = switch (a.getSeverity()) {
            case "KRITIK" -> "[KRITIK]";
            case "UYARI" -> "[UYARI]";
            default -> "[BILGI]";
        };
        return "%s %s - %s (%s = %s)".formatted(
                icon, a.getVehicleId(), a.getMessage(), a.getRule(), a.getValue());
    }

    private Set<String> severities() {
        if (allowedSeverities == null) {
            allowedSeverities = Arrays.stream(severitiesCsv.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        return allowedSeverities;
    }

    private String resolveQueueUrl() {
        if (alertsQueueUrl == null) {
            alertsQueueUrl = sqsClient.getQueueUrl(b -> b.queueName(alertsQueueName)).queueUrl();
        }
        return alertsQueueUrl;
    }

    private String resolveTopicArn() {
        if (notifyTopicArn == null) {
            notifyTopicArn = snsClient.createTopic(b -> b.name(notifyTopicName)).topicArn();
        }
        return notifyTopicArn;
    }
}
