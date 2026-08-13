package com.aracbakim.notification_service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Cooldown'i REDIS'te tutar -> instance'lar arasinda PAYLASILIR (dagitik).
 *
 * Puf noktasi: elle zaman hesabi YOK. markSent'te anahtari TTL ile yaziyoruz;
 * Redis anahtari cooldown suresi sonunda KENDISI siler. isInCooldown sadece
 * "anahtar hala var mi?" diye bakar. TTL, zaman mantigimizin yerine geciyor.
 */
public class RedisCooldownTracker implements CooldownTracker {

    private static final String PREFIX = "cooldown:"; // anahtar ad alani

    private final StringRedisTemplate redis;
    private final long cooldownSeconds;

    public RedisCooldownTracker(StringRedisTemplate redis, long cooldownSeconds) {
        this.redis = redis;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public boolean isInCooldown(String key) {
        // Anahtar duruyorsa cooldown surer; TTL dolunca Redis sildigi icin false doner.
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + key));
    }

    @Override
    public void markSent(String key) {
        // Deger onemsiz ("1"); onemli olan TTL: anahtar cooldownSeconds sonra silinir.
        redis.opsForValue().set(PREFIX + key, "1", Duration.ofSeconds(cooldownSeconds));
    }
}
