package com.example.cashflow.outbox.application;

import com.example.cashflow.eventbus.domain.DomainEvent;
import com.example.cashflow.eventbus.domain.EventBus;
import com.example.cashflow.outbox.domain.OutboxEvent;
import com.example.cashflow.outbox.domain.OutboxRepository;
import com.example.cashflow.shared.json.DomainEventJsonSerializer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final DomainEventJsonSerializer domainEventJsonSerializer;
    private final EventBus eventBus;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            DomainEventJsonSerializer domainEventJsonSerializer,
            EventBus eventBus,
            @Value("${app.outbox.batch-size:10}") int batchSize,
            @Value("${app.outbox.max-attempts:3}") int maxAttempts
    ) {
        this.outboxRepository = outboxRepository;
        this.domainEventJsonSerializer = domainEventJsonSerializer;
        this.eventBus = eventBus;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findPending(batchSize);
        for (OutboxEvent outboxEvent : events) {
            try {
                int currentAttempt = outboxEvent.getAttempts() + 1;
                log.info(
                        "Publishing outbox event: eventId={}, eventType={}, attempt={}",
                        outboxEvent.getId(),
                        outboxEvent.getEventType(),
                        currentAttempt
                );
                DomainEvent event = domainEventJsonSerializer.deserialize(
                        outboxEvent.getEventType(),
                        outboxEvent.getPayload()
                );
                eventBus.publish(event);
                outboxRepository.markAsPublished(outboxEvent.getId());
                log.info("Outbox event published: eventId={}", outboxEvent.getId());
            } catch (Exception exception) {
                handleFailure(outboxEvent, exception);
            }
        }
    }

    private void handleFailure(OutboxEvent outboxEvent, Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        if (outboxEvent.getAttempts() + 1 >= maxAttempts) {
            outboxRepository.markAsFailed(outboxEvent.getId(), message);
        } else {
            outboxRepository.incrementAttempts(outboxEvent.getId(), message);
        }
        log.error("Outbox event failed: eventId={}, error={}", outboxEvent.getId(), message);
    }
}
