package com.arac_bakim.query_api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE (Server-Sent Events) yayin merkezi.
 * Bagli tarayicilarin acik baglantilarini (SseEmitter) tutar; yeni bir uyari
 * geldiginde hepsine ITER (push). Boylece tarayici surekli sormaz (polling yok),
 * veri hazir oldugunda kendisine gelir.
 */
@Component
public class AlertBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(AlertBroadcaster.class);

    // Es zamanli erisim guvenli liste: yayin sirasinda baglanti eklenip cikabilir.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Yeni bir tarayici baglandiginda cagrilir; acik kalan bir kanal dondurur. */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L); // 0 = zaman asimi yok, kanal acik kalir
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        log.info("SSE baglanti eklendi. Toplam: {}", emitters.size());
        return emitter;
    }

    /** Gelen uyariyi tum bagli tarayicilara iter. */
    public void broadcast(String data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(data));
            } catch (Exception e) {
                emitters.remove(emitter); // kopmus baglantiyi at
            }
        }
    }
}
