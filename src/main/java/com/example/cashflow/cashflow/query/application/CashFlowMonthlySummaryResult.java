package com.example.cashflow.cashflow.query.application;

import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;

public record CashFlowMonthlySummaryResult(
        CashFlowMonthlySummaryView summary,
        String source
) {
}
