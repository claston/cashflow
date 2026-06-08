package com.example.cashflow.diagnostics.api;

import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import com.example.cashflow.cashflow.query.domain.CashFlowReadModelRepository;
import com.example.cashflow.diagnostics.application.ProjectionFailureModeService;
import com.example.cashflow.outbox.application.OutboxDiagnosticsService;
import com.example.cashflow.outbox.domain.OutboxStats;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final OutboxDiagnosticsService outboxDiagnosticsService;
    private final CashFlowReadModelRepository cashFlowReadModelRepository;
    private final ProjectionFailureModeService projectionFailureModeService;

    public DiagnosticsController(
            OutboxDiagnosticsService outboxDiagnosticsService,
            CashFlowReadModelRepository cashFlowReadModelRepository,
            ProjectionFailureModeService projectionFailureModeService
    ) {
        this.outboxDiagnosticsService = outboxDiagnosticsService;
        this.cashFlowReadModelRepository = cashFlowReadModelRepository;
        this.projectionFailureModeService = projectionFailureModeService;
    }

    @GetMapping("/outbox")
    public OutboxStats outboxStats() {
        return outboxDiagnosticsService.getStats();
    }

    @PostMapping("/outbox/reprocess-failed")
    public OutboxStats reprocessFailedOutboxEvents() {
        return outboxDiagnosticsService.reprocessFailed();
    }

    @GetMapping("/read-model")
    public Map<String, CashFlowMonthlySummaryView> readModel() {
        return cashFlowReadModelRepository.findAll();
    }

    @PostMapping("/read-model/clear")
    public void clearReadModel() {
        cashFlowReadModelRepository.clear();
    }

    @PostMapping("/projection/failure-mode")
    public Map<String, Boolean> setFailureMode(@Valid @RequestBody FailureModeRequest request) {
        return Map.of("enabled", projectionFailureModeService.setEnabled(request.enabled()));
    }
}
