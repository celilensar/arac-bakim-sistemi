package com.aracbakim.rules_engine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

/**
 * Esikleri thresholds tablosundan okuyup bellekte tutar (onbellek) ve
 * periyodik yeniler. Boylece tabloda bir esik degistirilince kural motoru
 * YENIDEN BASLATILMADAN, en gec bir yenileme periyodu sonra yeni degeri kullanir.
 *
 * Neden onbellek? Her telemetride tabloyu taramak yavas ve pahali olurdu;
 * bunun yerine listeyi bellekte tutup arada bir tazeliyoruz.
 */
@Component
public class ThresholdProvider {

    private static final Logger log = LoggerFactory.getLogger(ThresholdProvider.class);

    private final DynamoDbTable<Threshold> thresholdsTable;

    // volatile: yenileme baska thread'de olur; okuyan thread guncel listeyi gorsun.
    private volatile List<Threshold> cache = List.of();

    public ThresholdProvider(DynamoDbTable<Threshold> thresholdsTable) {
        this.thresholdsTable = thresholdsTable;
    }

    // Uygulama acilir acilmaz bir kez yukle (ilk mesaj gelmeden esikler hazir olsun).
    @PostConstruct
    public void init() {
        refresh();
    }

    // Periyodik yenileme (application.properties: app.thresholds.refresh-ms).
    @Scheduled(fixedDelayString = "${app.thresholds.refresh-ms}")
    public void refresh() {
        try {
            List<Threshold> fresh = thresholdsTable.scan().items().stream()
                    .filter(Threshold::isEnabled)   // sadece aktif kurallar
                    .toList();
            cache = fresh;
            log.info("Esikler yenilendi: {} aktif kural", fresh.size());
        } catch (Exception e) {
            log.error("Esikler yenilenemedi, eski onbellek korunuyor", e);
        }
    }

    public List<Threshold> getThresholds() {
        return cache;
    }
}
