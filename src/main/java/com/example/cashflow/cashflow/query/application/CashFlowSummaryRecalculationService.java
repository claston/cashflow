package com.example.cashflow.cashflow.query.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CashFlowSummaryRecalculationService {

    private final CashFlowEntryRepository cashFlowEntryRepository;
    private final Clock clock;

    public CashFlowSummaryRecalculationService(CashFlowEntryRepository cashFlowEntryRepository, Clock clock) {
        this.cashFlowEntryRepository = cashFlowEntryRepository;
        this.clock = clock;
    }

    public CashFlowMonthlySummaryView recalculate(UUID companyId, YearMonth month) {
        Instant now = Instant.now(clock);
        CashFlowMonthlySummaryView summary = CashFlowMonthlySummaryView.empty(companyId, month, now);
        for (CashFlowEntry entry : cashFlowEntryRepository.findByCompanyIdAndMonth(companyId, month)) {
            summary.applyEntry(entry, now);
        }
        return summary;
    }
}
