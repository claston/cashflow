package com.example.cashflow.cashflow.command.domain;

import com.example.cashflow.eventbus.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowEntryCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID companyId,
        String description,
        CashFlowEntryType type,
        BigDecimal amount,
        String category,
        String account,
        LocalDate dueDate,
        LocalDate paymentDate,
        CashFlowEntryStatus status,
        Instant occurredAt
) implements DomainEvent {

    public static CashFlowEntryCreatedEvent from(CashFlowEntry entry) {
        return new CashFlowEntryCreatedEvent(
                UUID.randomUUID(),
                entry.getId(),
                entry.getCompanyId(),
                entry.getDescription(),
                entry.getType(),
                entry.getAmount(),
                entry.getCategory(),
                entry.getAccount(),
                entry.getDueDate(),
                entry.getPaymentDate(),
                entry.getStatus(),
                entry.getCreatedAt()
        );
    }

    @Override
    public String eventType() {
        return "CashFlowEntryCreated";
    }
}
