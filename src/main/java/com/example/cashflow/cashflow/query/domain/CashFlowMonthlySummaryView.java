package com.example.cashflow.cashflow.query.domain;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryCreatedEvent;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

public class CashFlowMonthlySummaryView {

    private final UUID companyId;
    private final YearMonth month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal projectedBalance;
    private BigDecimal paidIncome;
    private BigDecimal paidExpense;
    private BigDecimal realizedBalance;
    private int entriesCount;
    private Instant lastUpdatedAt;

    private CashFlowMonthlySummaryView(
            UUID companyId,
            YearMonth month,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal projectedBalance,
            BigDecimal paidIncome,
            BigDecimal paidExpense,
            BigDecimal realizedBalance,
            int entriesCount,
            Instant lastUpdatedAt
    ) {
        this.companyId = companyId;
        this.month = month;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.projectedBalance = projectedBalance;
        this.paidIncome = paidIncome;
        this.paidExpense = paidExpense;
        this.realizedBalance = realizedBalance;
        this.entriesCount = entriesCount;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static CashFlowMonthlySummaryView empty(UUID companyId, YearMonth month, Instant now) {
        return new CashFlowMonthlySummaryView(
                companyId,
                month,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                now
        );
    }

    public static CashFlowMonthlySummaryView copyOf(CashFlowMonthlySummaryView summary) {
        return new CashFlowMonthlySummaryView(
                summary.companyId,
                summary.month,
                summary.totalIncome,
                summary.totalExpense,
                summary.projectedBalance,
                summary.paidIncome,
                summary.paidExpense,
                summary.realizedBalance,
                summary.entriesCount,
                summary.lastUpdatedAt
        );
    }

    public void apply(CashFlowEntryCreatedEvent event, Instant now) {
        applyEntryData(event.type(), event.amount(), event.status(), now);
    }

    public void applyEntry(CashFlowEntry entry, Instant now) {
        applyEntryData(entry.getType(), entry.getAmount(), entry.getStatus(), now);
    }

    private void applyEntryData(
            CashFlowEntryType type,
            BigDecimal amount,
            CashFlowEntryStatus status,
            Instant now
    ) {
        if (type == CashFlowEntryType.INCOME) {
            totalIncome = totalIncome.add(amount);
            if (status == CashFlowEntryStatus.PAID) {
                paidIncome = paidIncome.add(amount);
            }
        }
        if (type == CashFlowEntryType.EXPENSE) {
            totalExpense = totalExpense.add(amount);
            if (status == CashFlowEntryStatus.PAID) {
                paidExpense = paidExpense.add(amount);
            }
        }
        projectedBalance = totalIncome.subtract(totalExpense);
        realizedBalance = paidIncome.subtract(paidExpense);
        entriesCount = entriesCount + 1;
        lastUpdatedAt = now;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getProjectedBalance() {
        return projectedBalance;
    }

    public BigDecimal getPaidIncome() {
        return paidIncome;
    }

    public BigDecimal getPaidExpense() {
        return paidExpense;
    }

    public BigDecimal getRealizedBalance() {
        return realizedBalance;
    }

    public int getEntriesCount() {
        return entriesCount;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
