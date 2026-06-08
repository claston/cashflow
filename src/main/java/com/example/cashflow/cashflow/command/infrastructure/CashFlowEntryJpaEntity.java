package com.example.cashflow.cashflow.command.infrastructure;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "cashflow_entries",
        indexes = {
                @Index(name = "idx_cashflow_entries_company_due_date", columnList = "company_id,due_date"),
                @Index(name = "idx_cashflow_entries_company_payment_date", columnList = "company_id,payment_date"),
                @Index(name = "idx_cashflow_entries_company_status", columnList = "company_id,status")
        }
)
public class CashFlowEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashFlowEntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String account;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashFlowEntryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CashFlowEntryJpaEntity() {
    }

    public static CashFlowEntryJpaEntity fromDomain(CashFlowEntry entry) {
        CashFlowEntryJpaEntity entity = new CashFlowEntryJpaEntity();
        entity.id = entry.getId();
        entity.companyId = entry.getCompanyId();
        entity.description = entry.getDescription();
        entity.type = entry.getType();
        entity.amount = entry.getAmount();
        entity.category = entry.getCategory();
        entity.account = entry.getAccount();
        entity.dueDate = entry.getDueDate();
        entity.paymentDate = entry.getPaymentDate();
        entity.status = entry.getStatus();
        entity.createdAt = entry.getCreatedAt();
        entity.updatedAt = entry.getUpdatedAt();
        return entity;
    }

    public CashFlowEntry toDomain() {
        return CashFlowEntry.restore(
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
}
