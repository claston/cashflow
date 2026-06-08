package com.example.cashflow.ratelimit.application;

public interface RateLimiter {

    boolean allow(String clientId);
}
