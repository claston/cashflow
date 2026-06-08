package com.example.cashflow.cashflow.command.domain;

import com.example.cashflow.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class CashFlowEntry {

    private final UUID id;
    private final UUID companyId;
    private final String description;
    private final CashFlowEntryType type;
    private final BigDecimal amount;
    private final String category;
    private final String account;
    private final LocalDate dueDate;
    private final LocalDate paymentDate;
    private final CashFlowEntryStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CashFlowEntry(
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
            Instant createdAt,
            Instant updatedAt
    ) {
        validate(companyId, description, type, amount, category, account, dueDate, paymentDate, status);
        this.id = Objects.requireNonNull(id, "id is required");
        this.companyId = companyId;
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.account = account;
        this.dueDate = dueDate;
        this.paymentDate = paymentDate;
        this.status = status;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static CashFlowEntry create(
            UUID companyId,
            String description,
            CashFlowEntryType type,
            BigDecimal amount,
            String category,
            String account,
            LocalDate dueDate,
            LocalDate paymentDate,
            CashFlowEntryStatus status,
            Instant now
    ) {
        return new CashFlowEntry(
                UUID.randomUUID(),
                companyId,
                description,
                type,
                amount,
                category,
                account,
                dueDate,
                paymentDate,
                status,
                now,
                now
        );
    }

    public static CashFlowEntry restore(
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
            Instant createdAt,
            Instant updatedAt
    ) {
        return new CashFlowEntry(
                id,
                companyId,
                description,
                type,
                amount,
                category,
                account,
                dueDate,
                paymentDate,
                status,
                createdAt,
                updatedAt
        );
    }

    private static void validate(
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
        if (companyId == null) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "companyId is required.");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "description is required.");
        }
        if (type == null) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "type is required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "amount must be greater than zero.");
        }
        if (category == null || category.isBlank()) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "category is required.");
        }
        if (account == null || account.isBlank()) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "account is required.");
        }
        if (dueDate == null) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "dueDate is required.");
        }
        if (status == null) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "status is required.");
        }
        if (status == CashFlowEntryStatus.PAID && paymentDate == null) {
            throw new BusinessException("INVALID_CASHFLOW_ENTRY", "paymentDate is required when status is PAID.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getDescription() {
        return description;
    }

    public CashFlowEntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getAccount() {
        return account;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public CashFlowEntryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
