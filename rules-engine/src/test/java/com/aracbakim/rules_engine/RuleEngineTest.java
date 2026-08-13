package com.aracbakim.rules_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RuleEngine BIRIM testi.
 * Bagimlilik olan ThresholdProvider MOCK'lanir -> DynamoDB/AWS'ye hic gitmeden,
 * saf is mantigini (esik karsilastirma) izole dogruluyoruz. Hizli ve deterministik.
 */
@ExtendWith(MockitoExtension.class) // Mockito'yu JUnit'e bagla (@Mock'lari doldurur)
class RuleEngineTest {

    @Mock
    ThresholdProvider thresholdProvider; // sahte bagimlilik

    RuleEngine ruleEngine; // test edilen sinif (SUT: system under test)

    @BeforeEach
    void setUp() {
        // Her testten once taze bir RuleEngine kur (mock'u enjekte et).
        ruleEngine = new RuleEngine(thresholdProvider);
    }

    @Test
    @DisplayName("GT esigi asilinca uyari uretir")
    void gt_threshold_triggers_alert() {
        // Arrange: mock'a "sunlari dondur" de
        when(thresholdProvider.getThresholds())
                .thenReturn(List.of(threshold("ENGINE_OVERHEAT", "engineTemp", "GT", 105, "KRITIK", "Motor sicak")));

        // Act
        List<Alert> alerts = ruleEngine.evaluate(telemetry(t -> t.setEngineTemp(120)));

        // Assert
        assertThat(alerts).hasSize(1);
        Alert a = alerts.get(0);
        assertThat(a.getRule()).isEqualTo("ENGINE_OVERHEAT");
        assertThat(a.getSeverity()).isEqualTo("KRITIK");
        assertThat(a.getValue()).isEqualTo(120.0);
    }

    @Test
    @DisplayName("GT esigi asilmayinca uyari uretmez")
    void gt_threshold_not_triggered() {
        when(thresholdProvider.getThresholds())
                .thenReturn(List.of(threshold("ENGINE_OVERHEAT", "engineTemp", "GT", 105, "KRITIK", "Motor sicak")));

        List<Alert> alerts = ruleEngine.evaluate(telemetry(t -> t.setEngineTemp(90)));

        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("Tam sinir degerinde GT tetiklenmez (kesin buyuktur)")
    void gt_boundary_is_not_triggered() {
        when(thresholdProvider.getThresholds())
                .thenReturn(List.of(threshold("ENGINE_OVERHEAT", "engineTemp", "GT", 105, "KRITIK", "Motor sicak")));

        List<Alert> alerts = ruleEngine.evaluate(telemetry(t -> t.setEngineTemp(105))); // tam 105

        assertThat(alerts).isEmpty(); // 105 > 105 yanlis -> uyari yok
    }

    @Test
    @DisplayName("LT esigi altina dusunce uyari uretir")
    void lt_threshold_triggers_alert() {
        when(thresholdProvider.getThresholds())
                .thenReturn(List.of(threshold("OIL_LIFE_LOW", "oilLife", "LT", 15, "UYARI", "Yag dusuk")));

        List<Alert> alerts = ruleEngine.evaluate(telemetry(t -> t.setOilLife(10)));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualTo("UYARI");
    }

    @Test
    @DisplayName("Birden fazla esikten sadece asilanlar uyari uretir")
    void only_breached_thresholds_produce_alerts() {
        when(thresholdProvider.getThresholds()).thenReturn(List.of(
                threshold("ENGINE_OVERHEAT", "engineTemp", "GT", 105, "KRITIK", "Motor sicak"),
                threshold("OIL_LIFE_LOW", "oilLife", "LT", 15, "UYARI", "Yag dusuk"),
                threshold("BATTERY_LOW", "batteryVoltage", "LT", 12, "UYARI", "Aku dusuk")));

        List<Alert> alerts = ruleEngine.evaluate(telemetry(t -> {
            t.setEngineTemp(120);    // asar  -> KRITIK
            t.setOilLife(30);        // asmaz
            t.setBatteryVoltage(11); // asar  -> UYARI
        }));

        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(Alert::getRule)
                .containsExactlyInAnyOrder("ENGINE_OVERHEAT", "BATTERY_LOW");
    }

    @Test
    @DisplayName("alertId 'timestamp#ruleCode' formatinda (idempotent kimlik)")
    void alertId_is_timestamp_hash_ruleCode() {
        when(thresholdProvider.getThresholds())
                .thenReturn(List.of(threshold("ENGINE_OVERHEAT", "engineTemp", "GT", 105, "KRITIK", "Motor sicak")));

        List<Alert> alerts = ruleEngine.evaluate(
                telemetry(t -> { t.setEngineTemp(120); t.setTimestamp("2026-08-04T10:00:00Z"); }));

        assertThat(alerts.get(0).getAlertId()).isEqualTo("2026-08-04T10:00:00Z#ENGINE_OVERHEAT");
    }

    // ---------- yardimci kurucular (test verisi) ----------

    private Threshold threshold(String code, String metric, String op, double limit, String severity, String msg) {
        Threshold th = new Threshold();
        th.setRuleCode(code);
        th.setMetric(metric);
        th.setOperator(op);
        th.setLimitValue(limit);
        th.setSeverity(severity);
        th.setMessage(msg);
        th.setEnabled(true);
        return th;
    }

    // Saglikli varsayilan bir telemetri uretir; customizer ile istedigin alani bozarsin.
    private Telemetry telemetry(Consumer<Telemetry> customizer) {
        Telemetry t = new Telemetry();
        t.setVehicleId("VH-001");
        t.setTimestamp("2026-08-04T10:00:00Z");
        t.setEngineTemp(80);
        t.setOilLife(50);
        t.setTirePressure(35);
        t.setBatteryVoltage(12.6);
        t.setMileage(10000);
        customizer.accept(t);
        return t;
    }
}
