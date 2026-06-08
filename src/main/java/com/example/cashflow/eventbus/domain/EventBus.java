package com.example.cashflow.eventbus.domain;

import java.util.Optional;

public interface EventBus {

    void publish(DomainEvent event);

    Optional<DomainEvent> poll();
}
