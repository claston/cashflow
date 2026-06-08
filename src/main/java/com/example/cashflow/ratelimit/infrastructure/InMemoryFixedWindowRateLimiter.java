package com.example.cashflow.ratelimit.infrastructure;

import com.example.cashflow.ratelimit.application.RateLimitWindow;
import com.example.cashflow.ratelimit.application.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryFixedWindowRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int requestsPerMinute;

    public InMemoryFixedWindowRateLimiter(
            Clock clock,
            @Value("${app.rate-limit.requests-per-minute:60}") int requestsPerMinute
    ) {
        this.clock = clock;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public boolean allow(String clientId) {
        Instant now = Instant.now(clock);
        RateLimitWindow window = windows.computeIfAbsent(clientId, key -> new RateLimitWindow(now, 0));
        synchronized (window) {
            if (Duration.between(window.getWindowStart(), now).toSeconds() >= 60) {
                window.reset(now);
            }
            if (window.getCount() >= requestsPerMinute) {
                return false;
            }
            window.increment();
            return true;
        }
    }
}
