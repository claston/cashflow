package com.example.cashflow.cashflow.command.api;

import com.example.cashflow.cashflow.command.application.CreateCashFlowEntryCommand;
import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import org.springframework.stereotype.Component;

@Component
public class CashFlowEntryMapper {

    public CreateCashFlowEntryCommand toCommand(CreateCashFlowEntryRequest request) {
        return new CreateCashFlowEntryCommand(
                request.companyId(),
                request.description(),
                request.type(),
                request.amount(),
                request.category(),
                request.account(),
                request.dueDate(),
                request.paymentDate(),
                request.status()
        );
    }

    public CashFlowEntryResponse toResponse(CashFlowEntry entry) {
        return CashFlowEntryResponse.from(entry);
    }
}
