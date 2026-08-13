package com.aracbakim.notification_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * RedisCooldownTracker ENTEGRASYON testi.
 * Mock YOK: Testcontainers testin basinda Docker'da GERCEK bir Redis baslatir,
 * testler bitince kapatir. Boylece RedisCooldownTracker'in gercekten Redis'e
 * dogru yazip okudugunu (ve TTL'in isledigini) kanitlariz.
 *
 * NOT: Docker Desktop calisiyor olmali.
 */
@Testcontainers
class RedisCooldownTrackerIntegrationTest {

    // Tum test metotlari icin TEK Redis konteyneri (static) -> bir kez baslar.
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // Konteynerin rastgele atadigi host + port'a baglan.
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
    }

    @Test
    @DisplayName("Gercek Redis'e yazar ve cooldown'i okur")
    void writes_and_reads_from_real_redis() {
        RedisCooldownTracker tracker = new RedisCooldownTracker(redisTemplate, 120);
        String key = "VH-write#ENGINE_OVERHEAT";

        assertThat(tracker.isInCooldown(key)).isFalse(); // henuz yazilmadi
        tracker.markSent(key);
        assertThat(tracker.isInCooldown(key)).isTrue();  // Redis'te var
    }

    @Test
    @DisplayName("Farkli key birbirini etkilemez (gercek Redis)")
    void different_keys_are_independent() {
        RedisCooldownTracker tracker = new RedisCooldownTracker(redisTemplate, 120);

        tracker.markSent("VH-diffA#ENGINE_OVERHEAT");

        assertThat(tracker.isInCooldown("VH-diffA#ENGINE_OVERHEAT")).isTrue();  // yazilan
        assertThat(tracker.isInCooldown("VH-diffB#ENGINE_OVERHEAT")).isFalse(); // yazilmayan
    }

    @Test
    @DisplayName("TTL dolunca cooldown biter (Redis anahtari kendini siler)")
    void ttl_expiry_ends_cooldown() throws InterruptedException {
        RedisCooldownTracker tracker = new RedisCooldownTracker(redisTemplate, 1); // 1 sn cooldown
        String key = "VH-ttl#ENGINE_OVERHEAT";

        tracker.markSent(key);
        assertThat(tracker.isInCooldown(key)).isTrue();  // hemen sonra: var

        // Gercek Redis TTL'i gercek zamanda isler -> kisa bir bekleme kacinilmaz.
        // (Unit test'te Clock enjekte edip beklemiyorduk; entegrasyonun bedeli budur.)
        Thread.sleep(1500);

        assertThat(tracker.isInCooldown(key)).isFalse(); // anahtar silindi -> serbest
    }
}
