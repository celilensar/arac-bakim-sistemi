package com.aracbakim.rules_engine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Bakim kurallari burada. Bir telemetriyi alir, esikleri kontrol eder,
 * tetiklenen her kural icin bir Alert uretir (hic tetiklenmezse bos liste).
 *
 * Kurallari tek bir yerde topladik; yeni kural eklemek = buraya bir if daha.
 * (Ileride bu esikler application.properties'ten de okunabilir.)
 */
@Component
public class RuleEngine {

    public List<Alert> evaluate(Telemetry t) {
        List<Alert> alerts = new ArrayList<>();

        if (t.getEngineTemp() > 105) {
            alerts.add(build(t, "KRITIK", "ENGINE_OVERHEAT",
                    "Motor sicakligi cok yuksek", t.getEngineTemp()));
        }
        if (t.getOilLife() < 15) {
            alerts.add(build(t, "UYARI", "OIL_LIFE_LOW",
                    "Yag omru dusuk, bakim gerekli", t.getOilLife()));
        }
        if (t.getBatteryVoltage() < 12.0) {
            alerts.add(build(t, "UYARI", "BATTERY_LOW",
                    "Aku voltaji dusuk", t.getBatteryVoltage()));
        }
        if (t.getTirePressure() < 30) {
            alerts.add(build(t, "BILGI", "TIRE_PRESSURE_LOW",
                    "Lastik basinci dusuk", t.getTirePressure()));
        }

        return alerts;
    }

    private Alert build(Telemetry t, String severity, String ruleCode, String message, double value) {
        Alert a = new Alert();
        a.setVehicleId(t.getVehicleId());
        a.setTimestamp(t.getTimestamp());
        a.setAlertId(t.getTimestamp() + "#" + ruleCode); // ayni okuma+kural -> ayni id (idempotent)
        a.setSeverity(severity);
        a.setRule(ruleCode);
        a.setMessage(message);
        a.setValue(value);
        return a;
    }
}
