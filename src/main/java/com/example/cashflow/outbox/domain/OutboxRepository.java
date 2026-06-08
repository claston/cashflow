package com.example.cashflow.outbox.domain;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    List<OutboxEvent> findFailed(int limit);

    void markAsPublished(UUID eventId);

    void markAsFailed(UUID eventId, String errorMessage);

    void incrementAttempts(UUID eventId, String errorMessage);

    OutboxStats getStats();

    void resetFailedToPending();
}
