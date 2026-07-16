package com.aracbakim.ingestion_service;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Bir telemetri kaydinin DynamoDB'deki karsiligi.
 *
 * Iki isi birden gorur:
 *  1) Jackson, kuyruktan gelen JSON'u bu nesneye doldurur (getter/setter'lar sayesinde).
 *  2) DynamoDB Enhanced Client, bu nesneyi @DynamoDbBean anotasyonlarina bakip tabloya yazar.
 *
 * Enhanced Client bir "bean" ister: bos constructor + getter/setter'lar.
 * Anahtarlar getter uzerinde isaretlenir:
 *   @DynamoDbPartitionKey -> vehicleId   (tablodaki HASH anahtari)
 *   @DynamoDbSortKey      -> timestamp   (tablodaki RANGE anahtari)
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
