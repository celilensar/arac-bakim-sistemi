package com.aracbakim.notification_service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CooldownTracker BIRIM testi.
 * Mock yok, AWS yok. Zamani ELDE tutmak icin ilerletilebilir sahte saat kullaniyoruz
 * -> "cooldown suresi doldu" senaryosunu Thread.sleep olmadan test edebiliyoruz.
 */
class CooldownTrackerTest {

    // Ilerletilebilir sahte saat: instant()'i biz kontrol ediyoruz.
    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    @Test
    @DisplayName("Ilk gonderim cooldown'da degildir")
    void first_send_is_not_in_cooldown() {
        CooldownTracker tracker = new CooldownTracker(120, new TestClock());

        assertThat(tracker.isInCooldown("VH-001#ENGINE_OVERHEAT")).isFalse();
    }

    @Test
    @DisplayName("Gonderimden hemen sonra cooldown'dadir (tekrar engellenir)")
    void immediately_after_send_is_in_cooldown() {
        CooldownTracker tracker = new CooldownTracker(120, new TestClock());
        String key = "VH-001#ENGINE_OVERHEAT";

        tracker.markSent(key);

        assertThat(tracker.isInCooldown(key)).isTrue();
    }

    @Test
    @DisplayName("Cooldown suresi dolunca tekrar gonderilebilir")
    void after_cooldown_expires_is_allowed_again() {
        TestClock clock = new TestClock();
        CooldownTracker tracker = new CooldownTracker(120, clock);
        String key = "VH-001#ENGINE_OVERHEAT";

        tracker.markSent(key);
        assertThat(tracker.isInCooldown(key)).isTrue();  // hemen sonra: engelli

        clock.advanceSeconds(121);                       // 121 sn ileri sar
        assertThat(tracker.isInCooldown(key)).isFalse(); // sure doldu: serbest
    }

    @Test
    @DisplayName("Farkli arac/kural birbirini etkilemez")
    void different_keys_are_independent() {
        CooldownTracker tracker = new CooldownTracker(120, new TestClock());

        tracker.markSent("VH-001#ENGINE_OVERHEAT");

        assertThat(tracker.isInCooldown("VH-001#ENGINE_OVERHEAT")).isTrue();  // bu engelli
        assertThat(tracker.isInCooldown("VH-002#ENGINE_OVERHEAT")).isFalse(); // baska arac serbest
        assertThat(tracker.isInCooldown("VH-001#OIL_LIFE_LOW")).isFalse();    // baska kural serbest
    }
}
