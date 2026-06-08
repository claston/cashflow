package com.example.cashflow.cashflow.query.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import com.example.cashflow.cashflow.query.domain.CashFlowReadModelRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CashFlowSummaryQueryUseCaseTest {

    @Test
    void shouldReturnReadModelWhenPresent() {
        UUID companyId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 6);
        InMemoryReadModelRepository readModelRepository = new InMemoryReadModelRepository();
        readModelRepository.upsert(CashFlowMonthlySummaryView.empty(companyId, month, Instant.parse("2026-06-03T10:00:00Z")));
        CashFlowSummaryQueryUseCase useCase = new CashFlowSummaryQueryUseCase(
                readModelRepository,
                new CashFlowSummaryRecalculationService(new FakeCashFlowEntryRepository(List.of()), fixedClock())
        );

        CashFlowMonthlySummaryResult result = useCase.execute(companyId, month);

        assertThat(result.source()).isEqualTo("READ_MODEL");
    }

    @Test
    void shouldFallbackToTransactionalRecalculationWhenReadModelIsMissing() {
        UUID companyId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 6);
        CashFlowEntry entry = CashFlowEntry.create(
                companyId,
                "Venda",
                CashFlowEntryType.INCOME,
                new BigDecimal("25.00"),
                "SALES",
                "MAIN",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 10),
                CashFlowEntryStatus.PAID,
                Instant.parse("2026-06-03T10:00:00Z")
        );
        InMemoryReadModelRepository readModelRepository = new InMemoryReadModelRepository();
        CashFlowSummaryQueryUseCase useCase = new CashFlowSummaryQueryUseCase(
                readModelRepository,
                new CashFlowSummaryRecalculationService(new FakeCashFlowEntryRepository(List.of(entry)), fixedClock())
        );

        CashFlowMonthlySummaryResult result = useCase.execute(companyId, month);

        assertThat(result.source()).isEqualTo("FALLBACK_RECALCULATION");
        assertThat(result.summary().getTotalIncome()).isEqualByComparingTo("25.00");
        assertThat(readModelRepository.findMonthlySummary(companyId, month)).isPresent();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC);
    }

    private static class FakeCashFlowEntryRepository implements CashFlowEntryRepository {

        private final List<CashFlowEntry> entries;

        private FakeCashFlowEntryRepository(List<CashFlowEntry> entries) {
            this.entries = entries;
        }

        @Override
        public CashFlowEntry save(CashFlowEntry entry) {
            return entry;
        }

        @Override
        public List<CashFlowEntry> findByCompanyIdAndMonth(UUID companyId, YearMonth month) {
            return entries;
        }

        @Override
        public Optional<CashFlowEntry> findById(UUID id) {
            return Optional.empty();
        }
    }

    private static class InMemoryReadModelRepository implements CashFlowReadModelRepository {

        private final java.util.concurrent.ConcurrentHashMap<String, CashFlowMonthlySummaryView> storage = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public Optional<CashFlowMonthlySummaryView> findMonthlySummary(UUID companyId, YearMonth month) {
            return Optional.ofNullable(storage.get(companyId + ":" + month));
        }

        @Override
        public void upsert(CashFlowMonthlySummaryView summary) {
            storage.put(summary.getCompanyId() + ":" + summary.getMonth(), summary);
        }

        @Override
        public void clear() {
            storage.clear();
        }

        @Override
        public Map<String, CashFlowMonthlySummaryView> findAll() {
            return Map.copyOf(storage);
        }
    }
}
