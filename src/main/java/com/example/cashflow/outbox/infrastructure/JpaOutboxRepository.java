package com.example.cashflow.outbox.infrastructure;

import com.example.cashflow.outbox.domain.OutboxEvent;
import com.example.cashflow.outbox.domain.OutboxEventStatus;
import com.example.cashflow.outbox.domain.OutboxRepository;
import com.example.cashflow.outbox.domain.OutboxStats;
import com.example.cashflow.shared.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaOutboxRepository implements OutboxRepository {

    private final SpringDataOutboxJpaRepository repository;
    private final Clock clock;

    public JpaOutboxRepository(SpringDataOutboxJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        return repository.save(OutboxEventJpaEntity.fromDomain(event)).toDomain();
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, limit))
                .stream()
                .map(OutboxEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findFailed(int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED, PageRequest.of(0, limit))
                .stream()
                .map(OutboxEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void markAsPublished(UUID eventId) {
        OutboxEventJpaEntity entity = getEntity(eventId);
        entity.markAsPublished(Instant.now(clock));
    }

    @Override
    @Transactional
    public void markAsFailed(UUID eventId, String errorMessage) {
        OutboxEventJpaEntity entity = getEntity(eventId);
        entity.markAsFailed(errorMessage, Instant.now(clock));
    }

    @Override
    @Transactional
    public void incrementAttempts(UUID eventId, String errorMessage) {
        OutboxEventJpaEntity entity = getEntity(eventId);
        entity.incrementAttempts(errorMessage, Instant.now(clock));
    }

    @Override
    public OutboxStats getStats() {
        return new OutboxStats(
                repository.countByStatus(OutboxEventStatus.PENDING),
                repository.countByStatus(OutboxEventStatus.PUBLISHED),
                repository.countByStatus(OutboxEventStatus.FAILED)
        );
    }

    @Override
    @Transactional
    public void resetFailedToPending() {
        Instant now = Instant.now(clock);
        repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED)
                .forEach(entity -> entity.resetToPending(now));
    }

    private OutboxEventJpaEntity getEntity(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        "OUTBOX_EVENT_NOT_FOUND",
                        "Outbox event not found: " + eventId,
                        HttpStatus.NOT_FOUND
                ));
    }
}
