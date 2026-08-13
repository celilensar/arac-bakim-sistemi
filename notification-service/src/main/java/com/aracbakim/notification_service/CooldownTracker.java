package com.aracbakim.notification_service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (arac + kural) basina cooldown / rate-limit takibi.
 *
 * AWS'ye bagimli DEGIL -> saf, hizli, izole test edilebilir.
 * Clock disaridan verilir; boylece testte zamani ilerletip "sure doldu mu"
 * senaryosunu Thread.sleep olmadan dogrulayabiliriz.
 *
 * NOT: durum su an bellekte (tek instance icin yeterli). Cok instance'li
 * uretimde ayni arayuz Redis (TTL) ile degistirilebilir -> Faz B.
 */
public class CooldownTracker {

    private final Map<String, Instant> lastSentAt = new ConcurrentHashMap<>();
    private final long cooldownSeconds;
    private final Clock clock;

    // Uretimde kullanilan kurucu: gercek sistem saati.
    public CooldownTracker(long cooldownSeconds) {
        this(cooldownSeconds, Clock.systemUTC());
    }

    // Test icin: saat disaridan verilir (ilerletilebilir sahte saat).
    public CooldownTracker(long cooldownSeconds, Clock clock) {
        this.cooldownSeconds = cooldownSeconds;
        this.clock = clock;
    }

    /** key icin son gonderimden bu yana cooldown suresi HENUZ dolmadiysa true. */
    public boolean isInCooldown(String key) {
        Instant last = lastSentAt.get(key);
        return last != null && last.plusSeconds(cooldownSeconds).isAfter(clock.instant());
    }

    /** key icin "simdi bir bildirim gonderildi" bilgisini kaydet. */
    public void markSent(String key) {
        lastSentAt.put(key, clock.instant());
    }
}
