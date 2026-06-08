package com.example.cashflow.eventbus.infrastructure;

import com.example.cashflow.eventbus.domain.DomainEvent;
import com.example.cashflow.eventbus.domain.EventBus;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventBus implements EventBus {

    private final BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<>();

    @Override
    public void publish(DomainEvent event) {
        queue.offer(event);
    }

    @Override
    public Optional<DomainEvent> poll() {
        return Optional.ofNullable(queue.poll());
    }
}
