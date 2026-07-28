package com.aracbakim.rules_engine;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * "thresholds" tablosundaki bir kural satiri.
 * Kural artik koda gomulu degil; bu VERI uzerinden calisiyor.
 *
 * ornek: ENGINE_OVERHEAT / metric=engineTemp / operator=GT / limitValue=105 / severity=KRITIK
 *   => "engineTemp > 105 ise KRITIK uyari uret"
 */
@DynamoDbBean
public class Threshold {

    private String ruleCode;    // ENGINE_OVERHEAT gibi kural kimligi
    private String metric;      // telemetrinin hangi alani: engineTemp, oilLife...
    private String operator;    // GT (>) veya LT (<)
    private double limitValue;  // esik degeri
    private String severity;    // KRITIK / UYARI / BILGI
    private String message;     // uyari mesaji
    private boolean enabled;    // kural aktif mi

    @DynamoDbPartitionKey
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public double getLimitValue() { return limitValue; }
    public void setLimitValue(double limitValue) { this.limitValue = limitValue; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
