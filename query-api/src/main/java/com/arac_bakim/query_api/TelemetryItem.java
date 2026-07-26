package com.arac_bakim.query_api;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * telemetry tablosundaki bir satirin Java karsiligi.
 * ingestion-service'teki ile ayni yapida; burada tabloyu OKUMAK icin kullaniyoruz.
 * Enhanced Client, DynamoDB satirini bu nesneye doldurur; ayrica Jackson bunu
 * dogrudan JSON'a cevirip REST cevabinda dondurur.
 */
@DynamoDbBean
public class TelemetryItem {

    private String vehicleId;
    private String timestamp;
    private double engineTemp;
    private double oilLife;
    private double tirePressure;
    private double batteryVoltage;
    private long mileage;

    @DynamoDbPartitionKey
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    @DynamoDbSortKey
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getEngineTemp() {
        return engineTemp;
    }

    public void setEngineTemp(double engineTemp) {
        this.engineTemp = engineTemp;
    }

    public double getOilLife() {
        return oilLife;
    }

    public void setOilLife(double oilLife) {
        this.oilLife = oilLife;
    }

    public double getTirePressure() {
        return tirePressure;
    }

    public void setTirePressure(double tirePressure) {
        this.tirePressure = tirePressure;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public long getMileage() {
        return mileage;
    }

    public void setMileage(long mileage) {
        this.mileage = mileage;
    }
}
