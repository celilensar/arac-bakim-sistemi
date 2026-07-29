package com.arac_bakim.query_api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * dashboard-queue'yu tuketir ve her uyariyi AlertBroadcaster uzerinden
 * bagli tarayicilara SSE ile iter. Sonra mesaji siler.
 * (Kuyruk, alert-notifications topic'ine abone -> KRITIK/UYARI bildirim akisi.)
 */
@Component
public class DashboardConsumer {

    private final SqsClient sqsClient;
    private final AlertBroadcaster broadcaster;

    @Value("${app.sqs.dashboard-queue-name}")
    private String queueName;

    private String queueUrl;

    public DashboardConsumer(SqsClient sqsClient, AlertBroadcaster broadcaster) {
        this.sqsClient = sqsClient;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedDelayString = "${app.poll.interval-ms}")
    public void poll() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(b -> b
                .queueUrl(resolveQueueUrl())
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5));

        for (Message message : response.messages()) {
            broadcaster.broadcast(message.body());   // tarayicilara it
            sqsClient.deleteMessage(d -> d
                    .queueUrl(resolveQueueUrl())
                    .receiptHandle(message.receiptHandle()));
        }
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            queueUrl = sqsClient.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
        }
        return queueUrl;
    }
}
