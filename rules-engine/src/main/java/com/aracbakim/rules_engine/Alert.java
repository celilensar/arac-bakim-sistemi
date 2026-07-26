package com.aracbakim.rules_engine;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Bir bakim uyarisi.
 * Iki isi gorur: alerts tablosuna yazilir (@DynamoDbBean) ve alerts-queue'ya
 * JSON olarak basilir (Jackson getter'lar uzerinden).
 *
 * Anahtarlar: vehicleId (partition) + alertId (sort).
 * alertId = "<timestamp>#<ruleCode>" -> ayni okumadan ayni kural iki kez tetiklenirse
 * uzerine yazar (idempotent), farkli kurallar farkli alertId alir.
 */
@DynamoDbBean
public class Alert {

    private String vehicleId;
    private String alertId;
    private String timestamp;
    private String severity;   // KRITIK / UYARI / BILGI
    private String rule;       // ENGINE_OVERHEAT gibi kural kodu
    private String message;    // insan okunakli aciklama
    private double value;      // olcumun degeri

    @DynamoDbPartitionKey
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    @DynamoDbSortKey
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
