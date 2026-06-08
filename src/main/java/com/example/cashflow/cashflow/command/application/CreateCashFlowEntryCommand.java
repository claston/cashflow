package com.example.cashflow.cashflow.command.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCashFlowEntryCommand(
        UUID companyId,
        String description,
        CashFlowEntryType type,
        BigDecimal amount,
        String category,
        String account,
        LocalDate dueDate,
        LocalDate paymentDate,
        CashFlowEntryStatus status
) {
}
