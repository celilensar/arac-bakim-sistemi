package com.aracbakim.sensor_simulator;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS istemcilerini Spring "bean" olarak uretir.
 * Buradaki kritik nokta: endpointOverride ile SDK'yi gercek AWS yerine
 * lokaldeki LocalStack'e (localhost:4566) yonlendiriyoruz. AWS'e gecerken
 * tek yapacagimiz bu satiri kaldirmak/degistirmek olacak.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    // Bos birakilirsa (gercek AWS) endpointOverride yapilmaz.
    @Value("${aws.endpoint:}")
    private String endpoint;

    // Bos birakilirsa (gercek AWS) DefaultCredentialsProvider kullanilir (~/.aws, IAM rol...).
    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    private AwsCredentialsProvider credentials() {
        if (accessKey == null || accessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public SqsClient sqsClient() {
        var b = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials());
        if (endpoint != null && !endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint)); // dolu ise LocalStack'e yonlendir
        }
        return b.build();
    }
}
