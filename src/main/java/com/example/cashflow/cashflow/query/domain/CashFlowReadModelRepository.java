package com.example.cashflow.cashflow.query.domain;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CashFlowReadModelRepository {

    Optional<CashFlowMonthlySummaryView> findMonthlySummary(UUID companyId, YearMonth month);

    void upsert(CashFlowMonthlySummaryView summary);

    void clear();

    Map<String, CashFlowMonthlySummaryView> findAll();
}
