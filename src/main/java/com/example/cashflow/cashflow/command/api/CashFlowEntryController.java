package com.example.cashflow.cashflow.command.api;

import com.example.cashflow.cashflow.command.application.CreateCashFlowEntryUseCase;
import com.example.cashflow.cashflow.command.application.ListCashFlowEntriesUseCase;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cashflow-entries")
public class CashFlowEntryController {

    private final CreateCashFlowEntryUseCase createCashFlowEntryUseCase;
    private final ListCashFlowEntriesUseCase listCashFlowEntriesUseCase;
    private final CashFlowEntryMapper cashFlowEntryMapper;

    public CashFlowEntryController(
            CreateCashFlowEntryUseCase createCashFlowEntryUseCase,
            ListCashFlowEntriesUseCase listCashFlowEntriesUseCase,
            CashFlowEntryMapper cashFlowEntryMapper
    ) {
        this.createCashFlowEntryUseCase = createCashFlowEntryUseCase;
        this.listCashFlowEntriesUseCase = listCashFlowEntriesUseCase;
        this.cashFlowEntryMapper = cashFlowEntryMapper;
    }

    @PostMapping
    public CashFlowEntryResponse create(@Valid @RequestBody CreateCashFlowEntryRequest request) {
        return cashFlowEntryMapper.toResponse(
                createCashFlowEntryUseCase.execute(cashFlowEntryMapper.toCommand(request))
        );
    }

    @GetMapping
    public List<CashFlowEntryResponse> list(
            @RequestParam UUID companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return listCashFlowEntriesUseCase.execute(companyId, month)
                .stream()
                .map(cashFlowEntryMapper::toResponse)
                .toList();
    }
}
