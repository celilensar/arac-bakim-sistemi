package com.aracbakim.rules_engine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Kurallari UYGULAR ama artik kurallari icinde TUTMAZ.
 * Esikler ThresholdProvider'dan (yani thresholds tablosundan) gelir.
 * Her kural icin: metric alaninin degerini al -> operator+limit ile karsilastir -> asiyorsa uyari uret.
 */
@Component
public class RuleEngine {

    private final ThresholdProvider thresholdProvider;

    public RuleEngine(ThresholdProvider thresholdProvider) {
        this.thresholdProvider = thresholdProvider;
    }

    public List<Alert> evaluate(Telemetry t) {
        List<Alert> alerts = new ArrayList<>();

        for (Threshold th : thresholdProvider.getThresholds()) {
            double actual = metricValue(t, th.getMetric());
            boolean triggered = switch (th.getOperator()) {
                case "GT" -> actual > th.getLimitValue();   // greater than
                case "LT" -> actual < th.getLimitValue();   // less than
                default -> false;
            };
            if (triggered) {
                alerts.add(build(t, th, actual));
            }
        }
        return alerts;
    }

    // "engineTemp" gibi bir metric adini, telemetrinin ilgili degerine baglar.
    private double metricValue(Telemetry t, String metric) {
        return switch (metric) {
            case "engineTemp"     -> t.getEngineTemp();
            case "oilLife"        -> t.getOilLife();
            case "tirePressure"   -> t.getTirePressure();
            case "batteryVoltage" -> t.getBatteryVoltage();
            case "mileage"        -> t.getMileage();
            default -> throw new IllegalArgumentException("Bilinmeyen metric: " + metric);
        };
    }

    private Alert build(Telemetry t, Threshold th, double value) {
        Alert a = new Alert();
        a.setVehicleId(t.getVehicleId());
        a.setTimestamp(t.getTimestamp());
        a.setAlertId(t.getTimestamp() + "#" + th.getRuleCode()); // idempotent kimlik
        a.setSeverity(th.getSeverity());
        a.setRule(th.getRuleCode());
        a.setMessage(th.getMessage());
        a.setValue(value);
        return a;
    }
}
