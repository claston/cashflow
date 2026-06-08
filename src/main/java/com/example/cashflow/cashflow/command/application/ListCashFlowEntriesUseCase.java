package com.example.cashflow.cashflow.command.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListCashFlowEntriesUseCase {

    private final CashFlowEntryRepository cashFlowEntryRepository;

    public ListCashFlowEntriesUseCase(CashFlowEntryRepository cashFlowEntryRepository) {
        this.cashFlowEntryRepository = cashFlowEntryRepository;
    }

    public List<CashFlowEntry> execute(UUID companyId, YearMonth month) {
        return cashFlowEntryRepository.findByCompanyIdAndMonth(companyId, month);
    }
}
