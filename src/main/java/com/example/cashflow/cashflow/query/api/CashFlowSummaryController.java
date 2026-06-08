package com.example.cashflow.cashflow.query.api;

import com.example.cashflow.cashflow.query.application.CashFlowSummaryQueryUseCase;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cashflow-summary")
public class CashFlowSummaryController {

    private final CashFlowSummaryQueryUseCase cashFlowSummaryQueryUseCase;

    public CashFlowSummaryController(CashFlowSummaryQueryUseCase cashFlowSummaryQueryUseCase) {
        this.cashFlowSummaryQueryUseCase = cashFlowSummaryQueryUseCase;
    }

    @GetMapping("/monthly")
    public CashFlowMonthlySummaryResponse getMonthlySummary(
            @RequestParam UUID companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return CashFlowMonthlySummaryResponse.from(
                cashFlowSummaryQueryUseCase.execute(companyId, month)
        );
    }
}
