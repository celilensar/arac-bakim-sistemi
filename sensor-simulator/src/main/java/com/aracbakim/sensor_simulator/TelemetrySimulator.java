package com.aracbakim.sensor_simulator;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Periyodik olarak (application.properties'teki interval kadar) her arac icin
 * sahte telemetri uretir ve JSON olarak SQS kuyruguna basar.
 *
 * @Component  -> Spring bu sinifi otomatik bulup nesnesini olusturur.
 * @Scheduled  -> metodu belirli araliklarla otomatik calistirir (zamanlanmis gorev).
 */
@Component
public class TelemetrySimulator {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySimulator.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.sqs.queue-name}")
    private String queueName;

    @Value("${app.simulator.vehicle-count}")
    private int vehicleCount;

    // Kuyruk URL'sini bir kez cozup saklayacagiz (her seferinde sormamak icin).
    private String queueUrl;

    // Spring, SqsClient bean'ini bu constructor uzerinden enjekte eder (dependency injection).
    public TelemetrySimulator(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Scheduled(fixedRateString = "${app.simulator.interval-ms}")
    public void produce() {
        for (int i = 1; i <= vehicleCount; i++) {
            String vehicleId = String.format("VH-%03d", i); // VH-001, VH-002, ...
            Telemetry t = randomTelemetry(vehicleId);
            send(t);
        }
    }

    private Telemetry randomTelemetry(String vehicleId) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Telemetry(
                vehicleId,
                Instant.now().toString(),
                round(r.nextDouble(70, 110)),   // motor sicakligi 70-110 C
                round(r.nextDouble(5, 100)),    // yag omru 5-100 %
                round(r.nextDouble(28, 36)),    // lastik basinci 28-36 PSI
                round(r.nextDouble(11.5, 14.5)),// aku voltaji 11.5-14.5 V
                r.nextLong(10_000, 200_000)     // km
        );
    }

    private void send(Telemetry t) {
        try {
            String json = objectMapper.writeValueAsString(t);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(resolveQueueUrl())
                    .messageBody(json)
                    .build());
            log.info("Gonderildi -> {}", json);
        } catch (Exception e) {
            log.error("Telemetri gonderilemedi: {}", t.vehicleId(), e);
        }
    }

    // Kuyrugun adindan tam URL'sini cozer (ilk cagrida sorar, sonra saklar).
    private String resolveQueueUrl() {
        if (queueUrl == null) {
            queueUrl = sqsClient.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
            log.info("Kuyruk URL cozuldu: {}", queueUrl);
        }
        return queueUrl;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0; // tek ondalik
    }
}
