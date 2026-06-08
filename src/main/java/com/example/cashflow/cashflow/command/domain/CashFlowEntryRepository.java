package com.example.cashflow.cashflow.command.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashFlowEntryRepository {

    CashFlowEntry save(CashFlowEntry entry);

    List<CashFlowEntry> findByCompanyIdAndMonth(UUID companyId, YearMonth month);

    Optional<CashFlowEntry> findById(UUID id);
}
