package com.aracbakim.notification_service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldown'i BELLEKTE tutar (ConcurrentHashMap). Tek instance icin yeterli.
 * Clock enjekte edilebilir -> zaman-bagimli mantik Thread.sleep'siz test edilir.
 */
public class InMemoryCooldownTracker implements CooldownTracker {

    private final Map<String, Instant> lastSentAt = new ConcurrentHashMap<>();
    private final long cooldownSeconds;
    private final Clock clock;

    public InMemoryCooldownTracker(long cooldownSeconds) {
        this(cooldownSeconds, Clock.systemUTC());
    }

    public InMemoryCooldownTracker(long cooldownSeconds, Clock clock) {
        this.cooldownSeconds = cooldownSeconds;
        this.clock = clock;
    }

    @Override
    public boolean isInCooldown(String key) {
        Instant last = lastSentAt.get(key);
        return last != null && last.plusSeconds(cooldownSeconds).isAfter(clock.instant());
    }

    @Override
    public void markSent(String key) {
        lastSentAt.put(key, clock.instant());
    }
}
