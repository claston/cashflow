package com.example.cashflow.cashflow.command.api;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowEntryResponse(
        UUID id,
        UUID companyId,
        String description,
        CashFlowEntryType type,
        BigDecimal amount,
        String category,
        String account,
        LocalDate dueDate,
        LocalDate paymentDate,
        CashFlowEntryStatus status,
        Instant createdAt
) {

    public static CashFlowEntryResponse from(CashFlowEntry entry) {
        return new CashFlowEntryResponse(
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
}
