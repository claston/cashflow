package com.example.cashflow.cashflow.query.application;

import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import com.example.cashflow.cashflow.query.domain.CashFlowReadModelRepository;
import java.time.YearMonth;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CashFlowSummaryQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CashFlowSummaryQueryUseCase.class);

    private final CashFlowReadModelRepository readModelRepository;
    private final CashFlowSummaryRecalculationService recalculationService;

    public CashFlowSummaryQueryUseCase(
            CashFlowReadModelRepository readModelRepository,
            CashFlowSummaryRecalculationService recalculationService
    ) {
        this.readModelRepository = readModelRepository;
        this.recalculationService = recalculationService;
    }

    public CashFlowMonthlySummaryResult execute(UUID companyId, YearMonth month) {
        return readModelRepository.findMonthlySummary(companyId, month)
                .map(view -> new CashFlowMonthlySummaryResult(view, "READ_MODEL"))
                .orElseGet(() -> {
                    log.info(
                            "Read model not found. Recalculating summary from transactional database. companyId={}, month={}",
                            companyId,
                            month
                    );
                    CashFlowMonthlySummaryView recalculated = recalculationService.recalculate(companyId, month);
                    readModelRepository.upsert(recalculated);
                    return new CashFlowMonthlySummaryResult(recalculated, "FALLBACK_RECALCULATION");
                });
    }
}
