package com.example.cashflow.ratelimit.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFixedWindowRateLimiterTest {

    @Test
    void shouldAllowUntilLimitAndBlockAfterwards() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-03T10:00:00Z"));
        InMemoryFixedWindowRateLimiter rateLimiter = new InMemoryFixedWindowRateLimiter(clock, 2);

        assertThat(rateLimiter.allow("client-1")).isTrue();
        assertThat(rateLimiter.allow("client-1")).isTrue();
        assertThat(rateLimiter.allow("client-1")).isFalse();

        clock.setInstant(Instant.parse("2026-06-03T10:01:01Z"));

        assertThat(rateLimiter.allow("client-1")).isTrue();
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}
