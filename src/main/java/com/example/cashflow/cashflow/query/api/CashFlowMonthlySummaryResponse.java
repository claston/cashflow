package com.example.cashflow.cashflow.query.api;

import com.example.cashflow.cashflow.query.application.CashFlowMonthlySummaryResult;
import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashFlowMonthlySummaryResponse(
        UUID companyId,
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal projectedBalance,
        BigDecimal paidIncome,
        BigDecimal paidExpense,
        BigDecimal realizedBalance,
        int entriesCount,
        Instant lastUpdatedAt,
        String source
) {

    public static CashFlowMonthlySummaryResponse from(CashFlowMonthlySummaryResult result) {
        CashFlowMonthlySummaryView summary = result.summary();
        return new CashFlowMonthlySummaryResponse(
                summary.getCompanyId(),
                summary.getMonth().toString(),
                summary.getTotalIncome(),
                summary.getTotalExpense(),
                summary.getProjectedBalance(),
                summary.getPaidIncome(),
                summary.getPaidExpense(),
                summary.getRealizedBalance(),
                summary.getEntriesCount(),
                summary.getLastUpdatedAt(),
                result.source()
        );
    }
}
