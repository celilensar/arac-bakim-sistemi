package com.aracbakim.sensor_simulator;

/**
 * Bir aracin tek bir anlik sensor okumasi.
 * Java "record": sadece veri tasiyan, degismez (immutable) kucuk bir sinif.
 * Jackson bunu otomatik olarak JSON'a cevirebilir.
 *
 * timestamp'i String tuttuk (Instant.now().toString() -> ISO-8601),
 * boylece JSON'a cevirirken ekstra Jackson modulu gerekmez ve DynamoDB
 * sort key (RANGE) olarak dogrudan kullanilabilir.
 */
public record Telemetry(
        String vehicleId,
        String timestamp,
        double engineTemp,      // motor sicakligi (C)
        double oilLife,         // yag omru (%)
        double tirePressure,    // lastik basinci (PSI)
        double batteryVoltage,  // aku voltaji (V)
        long mileage            // kilometre
) {
}
