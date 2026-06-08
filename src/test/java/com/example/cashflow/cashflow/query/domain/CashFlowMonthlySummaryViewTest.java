package com.example.cashflow.cashflow.query.domain;

import com.example.cashflow.cashflow.command.domain.CashFlowEntryCreatedEvent;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CashFlowMonthlySummaryViewTest {

    @Test
    void shouldApplyIncomeAndExpenseValuesCorrectly() {
        UUID companyId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-03T10:00:00Z");
        CashFlowMonthlySummaryView summary = CashFlowMonthlySummaryView.empty(companyId, YearMonth.of(2026, 6), now);

        summary.apply(event(companyId, CashFlowEntryType.INCOME, "100.00", CashFlowEntryStatus.PAID), now);
        summary.apply(event(companyId, CashFlowEntryType.EXPENSE, "40.00", CashFlowEntryStatus.PAID), now);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("100.00");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("40.00");
        assertThat(summary.getProjectedBalance()).isEqualByComparingTo("60.00");
        assertThat(summary.getPaidIncome()).isEqualByComparingTo("100.00");
        assertThat(summary.getPaidExpense()).isEqualByComparingTo("40.00");
        assertThat(summary.getRealizedBalance()).isEqualByComparingTo("60.00");
        assertThat(summary.getEntriesCount()).isEqualTo(2);
    }

    private CashFlowEntryCreatedEvent event(
            UUID companyId,
            CashFlowEntryType type,
            String amount,
            CashFlowEntryStatus status
    ) {
        return new CashFlowEntryCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                companyId,
                "Entry",
                type,
                new BigDecimal(amount),
                "CAT",
                "ACC",
                LocalDate.of(2026, 6, 10),
                status == CashFlowEntryStatus.PAID ? LocalDate.of(2026, 6, 10) : null,
                status,
                Instant.parse("2026-06-03T10:00:00Z")
        );
    }
}
