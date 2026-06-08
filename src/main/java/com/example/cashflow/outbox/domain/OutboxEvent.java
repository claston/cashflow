package com.example.cashflow.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public final class OutboxEvent {

    private final UUID id;
    private final UUID aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String payload;
    private final OutboxEventStatus status;
    private final int attempts;
    private final String lastError;
    private final Instant createdAt;
    private final Instant publishedAt;
    private final Instant updatedAt;

    private OutboxEvent(
            UUID id,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload,
            OutboxEventStatus status,
            int attempts,
            String lastError,
            Instant createdAt,
            Instant publishedAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
    }

    public static OutboxEvent createPending(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload,
            Instant now
    ) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateId,
                aggregateType,
                eventType,
                payload,
                OutboxEventStatus.PENDING,
                0,
                null,
                now,
                null,
                now
        );
    }

    public static OutboxEvent restore(
            UUID id,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload,
            OutboxEventStatus status,
            int attempts,
            String lastError,
            Instant createdAt,
            Instant publishedAt,
            Instant updatedAt
    ) {
        return new OutboxEvent(
                id,
                aggregateId,
                aggregateType,
                eventType,
                payload,
                status,
                attempts,
                lastError,
                createdAt,
                publishedAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
