package com.example.cashflow.cashflow.command.api;

import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCashFlowEntryRequest(
        @NotNull UUID companyId,
        @NotBlank String description,
        @NotNull CashFlowEntryType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String category,
        @NotBlank String account,
        @NotNull LocalDate dueDate,
        LocalDate paymentDate,
        @NotNull CashFlowEntryStatus status
) {
}
