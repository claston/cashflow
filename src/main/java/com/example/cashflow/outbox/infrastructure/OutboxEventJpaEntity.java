package com.example.cashflow.outbox.infrastructure;

import com.example.cashflow.outbox.domain.OutboxEvent;
import com.example.cashflow.outbox.domain.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_events_status_created_at", columnList = "status,created_at")
        }
)
public class OutboxEventJpaEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attempts;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxEventJpaEntity() {
    }

    public static OutboxEventJpaEntity fromDomain(OutboxEvent event) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.id = event.getId();
        entity.aggregateId = event.getAggregateId();
        entity.aggregateType = event.getAggregateType();
        entity.eventType = event.getEventType();
        entity.payload = event.getPayload();
        entity.status = event.getStatus();
        entity.attempts = event.getAttempts();
        entity.lastError = event.getLastError();
        entity.createdAt = event.getCreatedAt();
        entity.publishedAt = event.getPublishedAt();
        entity.updatedAt = event.getUpdatedAt();
        return entity;
    }

    public OutboxEvent toDomain() {
        return OutboxEvent.restore(
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

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public UUID getId() {
        return id;
    }

    public void markAsPublished(Instant now) {
        status = OutboxEventStatus.PUBLISHED;
        publishedAt = now;
        updatedAt = now;
        lastError = null;
    }

    public void incrementAttempts(String errorMessage, Instant now) {
        attempts = attempts + 1;
        lastError = errorMessage;
        updatedAt = now;
    }

    public void markAsFailed(String errorMessage, Instant now) {
        attempts = attempts + 1;
        status = OutboxEventStatus.FAILED;
        lastError = errorMessage;
        updatedAt = now;
    }

    public void resetToPending(Instant now) {
        status = OutboxEventStatus.PENDING;
        attempts = 0;
        lastError = null;
        updatedAt = now;
    }
}
