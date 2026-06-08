package com.example.cashflow.outbox.domain;

public record OutboxStats(
        long pending,
        long published,
        long failed
) {
}
