package com.aracbakim.rules_engine;

/**
 * rules-queue'dan gelen telemetri JSON'unu okumak icin kullanilan nesne.
 * Jackson getter/setter'lar uzerinden doldurur.
 * (DynamoDB anotasyonu yok; bu servis telemetriyi yazmaz, sadece okuyup kural uygular.)
 */
public class Telemetry {

    private String vehicleId;
    private String timestamp;
    private double engineTemp;
    private double oilLife;
    private double tirePressure;
    private double batteryVoltage;
    private long mileage;

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getEngineTemp() { return engineTemp; }
    public void setEngineTemp(double engineTemp) { this.engineTemp = engineTemp; }

    public double getOilLife() { return oilLife; }
    public void setOilLife(double oilLife) { this.oilLife = oilLife; }

    public double getTirePressure() { return tirePressure; }
    public void setTirePressure(double tirePressure) { this.tirePressure = tirePressure; }

    public double getBatteryVoltage() { return batteryVoltage; }
    public void setBatteryVoltage(double batteryVoltage) { this.batteryVoltage = batteryVoltage; }

    public long getMileage() { return mileage; }
    public void setMileage(long mileage) { this.mileage = mileage; }
}
