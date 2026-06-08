package com.example.cashflow.eventbus.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    UUID aggregateId();

    String eventType();

    Instant occurredAt();
}
