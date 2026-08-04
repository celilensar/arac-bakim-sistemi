package com.aracbakim.ingestion_service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS istemcilerini Spring bean olarak uretir.
 * Simulatordeki gibi endpointOverride ile LocalStack'e (localhost:4566) yonlendirilir.
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

    @Value("${app.dynamodb.table-name}")
    private String tableName;

    // access-key doluysa sabit anahtar (LocalStack), bos ise ortamin varsayilan kimlik zinciri.
    private AwsCredentialsProvider credentials() {
        if (accessKey == null || accessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    // endpoint doluysa LocalStack'e yonlendir; bos ise gercek AWS adresini kullan.
    private boolean hasEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    @Bean
    public SqsClient sqsClient() {
        var b = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials());
        if (hasEndpoint()) {
            b.endpointOverride(URI.create(endpoint));
        }
        return b.build();
    }

    // SNS istemcisi: telemetriyi topic'e yayinlamak (fan-out) icin.
    @Bean
    public SnsClient snsClient() {
        var b = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials());
        if (hasEndpoint()) {
            b.endpointOverride(URI.create(endpoint));
        }
        return b.build();
    }

    // Dusuk seviyeli DynamoDB istemcisi (Enhanced Client bunu icinde kullanir).
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var b = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials());
        if (hasEndpoint()) {
            b.endpointOverride(URI.create(endpoint));
        }
        return b.build();
    }

    // Enhanced Client: Java nesnesi <-> DynamoDB satiri donusumunu yapar.
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    // "telemetry" tablosunun, TelemetryItem sinifi uzerinden nesne temsili.
    // Consumer bu bean'i kullanarak putItem yapacak.
    @Bean
    public DynamoDbTable<TelemetryItem> telemetryTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(tableName, TableSchema.fromBean(TelemetryItem.class));
    }
}
