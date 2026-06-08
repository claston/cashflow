package com.example.cashflow.outbox.domain;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
