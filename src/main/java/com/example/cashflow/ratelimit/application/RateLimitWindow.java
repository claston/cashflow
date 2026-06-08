package com.example.cashflow.ratelimit.application;

import java.time.Instant;

public class RateLimitWindow {

    private Instant windowStart;
    private int count;

    public RateLimitWindow(Instant windowStart, int count) {
        this.windowStart = windowStart;
        this.count = count;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public int getCount() {
        return count;
    }

    public void reset(Instant newWindowStart) {
        this.windowStart = newWindowStart;
        this.count = 0;
    }

    public void increment() {
        this.count = count + 1;
    }
}
