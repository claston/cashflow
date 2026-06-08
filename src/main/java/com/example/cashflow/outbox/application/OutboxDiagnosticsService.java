package com.example.cashflow.outbox.application;

import com.example.cashflow.outbox.domain.OutboxRepository;
import com.example.cashflow.outbox.domain.OutboxStats;
import org.springframework.stereotype.Service;

@Service
public class OutboxDiagnosticsService {

    private final OutboxRepository outboxRepository;

    public OutboxDiagnosticsService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    public OutboxStats getStats() {
        return outboxRepository.getStats();
    }

    public OutboxStats reprocessFailed() {
        outboxRepository.resetFailedToPending();
        return outboxRepository.getStats();
    }
}
