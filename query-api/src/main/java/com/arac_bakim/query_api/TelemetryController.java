package com.arac_bakim.query_api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/**
 * Telemetri verisini REST ile disariya acar.
 *
 * @RestController -> bu sinifin donen degerleri dogrudan HTTP cevabi (JSON) olur.
 *
 * Onemli: burada DynamoDB "query" kullaniyoruz, "scan" degil.
 *   - scan  = tum tabloyu tarar (pahali).
 *   - query = partition key ile dogrudan ilgili araca gider (hizli).
 * Iste vehicleId'yi partition key yapmamizin pratik faydasi bu.
 */
@RestController
public class TelemetryController {

    private final DynamoDbTable<TelemetryItem> telemetryTable;

    public TelemetryController(DynamoDbTable<TelemetryItem> telemetryTable) {
        this.telemetryTable = telemetryTable;
    }

    /**
     * Bir aracin en son N telemetrisi (varsayilan 10).
     * Ornek: GET http://localhost:8080/vehicles/VH-001/telemetry?limit=5
     */
    @GetMapping("/vehicles/{vehicleId}/telemetry")
    public List<TelemetryItem> getTelemetry(
            @PathVariable String vehicleId,
            @RequestParam(defaultValue = "10") int limit) {

        // "vehicleId = :v" kosulu: sadece bu aracin bolmesini oku
        QueryConditional condition = QueryConditional.keyEqualTo(
                k -> k.partitionValue(vehicleId));

        return telemetryTable.query(r -> r
                        .queryConditional(condition)
                        .scanIndexForward(false)  // sort key'e gore TERS sirala = en yeni once
                        .limit(limit))
                .items().stream()
                .limit(limit)
                .toList();
    }

    /**
     * Bir aracin sadece en son (guncel) telemetrisi.
     * Ornek: GET http://localhost:8080/vehicles/VH-001/latest
     */
    @GetMapping("/vehicles/{vehicleId}/latest")
    public TelemetryItem getLatest(@PathVariable String vehicleId) {
        List<TelemetryItem> latest = getTelemetry(vehicleId, 1);
        return latest.isEmpty() ? null : latest.get(0);
    }
}
