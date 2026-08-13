package com.aracbakim.notification_service;

/**
 * (arac + kural) basina cooldown takibi icin SOZLESME (arayuz).
 *
 * Iki uygulamasi var:
 *   - InMemoryCooldownTracker : tek instance (ConcurrentHashMap)
 *   - RedisCooldownTracker    : dagitik (Redis + TTL), cok instance'ta calisir
 *
 * NotificationConsumer sadece bu arayuze bagli; hangi uygulamanin
 * kullanilacagina config karar verir (Strategy pattern).
 */
public interface CooldownTracker {

    /** key icin cooldown suresi henuz dolmadiysa true. */
    boolean isInCooldown(String key);

    /** key icin "simdi bir bildirim gonderildi" bilgisini kaydet. */
    void markSent(String key);
}
