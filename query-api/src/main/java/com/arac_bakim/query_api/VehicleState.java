package com.arac_bakim.query_api;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * vehicle_state tablosundaki bir satir = aracin en guncel durumu.
 * Dashboard'daki filo kartlari bunu /api/fleet ile okur.
 */
@DynamoDbBean
public class VehicleState {

    private String vehicleId;
    private String timestamp;
    private double engineTemp;
    private double oilLife;
    private double tirePressure;
    private double batteryVoltage;
    private long mileage;

    @DynamoDbPartitionKey
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
