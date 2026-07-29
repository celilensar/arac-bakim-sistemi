package com.arac_bakim.query_api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

/**
 * Dashboard'un konustugu uclar:
 *   GET /api/fleet          -> araclarin guncel durumu (ilk yukleme icin)
 *   GET /api/stream/alerts  -> SSE canli uyari akisi (surekli acik kanal)
 */
@RestController
public class DashboardController {

    private final DynamoDbTable<VehicleState> vehicleStateTable;
    private final AlertBroadcaster broadcaster;

    public DashboardController(DynamoDbTable<VehicleState> vehicleStateTable,
                               AlertBroadcaster broadcaster) {
        this.vehicleStateTable = vehicleStateTable;
        this.broadcaster = broadcaster;
    }

    /** Filodaki tum araclarin guncel durumu. */
    @GetMapping("/api/fleet")
    public List<VehicleState> fleet() {
        return vehicleStateTable.scan().items().stream().toList();
    }

    /**
     * Canli uyari akisi. Tarayici bu uca baglanip ACIK tutar; sunucu yeni uyari
     * geldikce buraya iter. produces=text/event-stream -> SSE protokolu.
     */
    @GetMapping(value = "/api/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        return broadcaster.register();
    }
}
