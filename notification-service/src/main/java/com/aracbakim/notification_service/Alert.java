package com.aracbakim.notification_service;

/**
 * alerts-queue'dan gelen uyari JSON'unu okumak icin kullanilan nesne.
 * (rules-engine'in urettigi Alert ile ayni alanlar; burada sadece OKUYUP bildirime ceviriyoruz.)
 */
public class Alert {

    private String vehicleId;
    private String alertId;
    private String timestamp;
    private String severity;
    private String rule;
    private String message;
    private double value;

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

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
