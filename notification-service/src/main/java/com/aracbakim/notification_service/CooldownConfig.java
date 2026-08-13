package com.aracbakim.notification_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Hangi CooldownTracker uygulamasinin kullanilacagina KARAR VERIR.
 * app.cooldown.store = redis  -> RedisCooldownTracker (dagitik)
 * app.cooldown.store = memory -> InMemoryCooldownTracker (veya ayar yoksa: varsayilan)
 *
 * NotificationConsumer sadece CooldownTracker arayuzunu ister; hangi somut
 * sinifin geldigini bilmez (Strategy pattern + Spring @ConditionalOnProperty).
 */
@Configuration
public class CooldownConfig {

    @Value("${app.notify.cooldown-seconds}")
    private long cooldownSeconds;

    @Bean
    @ConditionalOnProperty(name = "app.cooldown.store", havingValue = "redis")
    public CooldownTracker redisCooldownTracker(StringRedisTemplate redis) {
        return new RedisCooldownTracker(redis, cooldownSeconds);
    }

    @Bean
    @ConditionalOnProperty(name = "app.cooldown.store", havingValue = "memory", matchIfMissing = true)
    public CooldownTracker inMemoryCooldownTracker() {
        return new InMemoryCooldownTracker(cooldownSeconds);
    }
}
