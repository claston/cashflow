package com.example.cashflow.cashflow.command.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryStatus;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryType;
import com.example.cashflow.outbox.domain.OutboxEvent;
import com.example.cashflow.outbox.domain.OutboxRepository;
import com.example.cashflow.outbox.domain.OutboxStats;
import com.example.cashflow.shared.exception.BusinessException;
import com.example.cashflow.shared.json.DomainEventJsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCashFlowEntryUseCaseTest {

    @Test
    void shouldCreateEntryAndOutboxEvent() {
        InMemoryCashFlowEntryRepository cashFlowEntryRepository = new InMemoryCashFlowEntryRepository();
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        CreateCashFlowEntryUseCase useCase = new CreateCashFlowEntryUseCase(
                cashFlowEntryRepository,
                outboxRepository,
                new DomainEventJsonSerializer(new ObjectMapper().findAndRegisterModules()),
                Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC)
        );

        CashFlowEntry entry = useCase.execute(validCommand());

        assertThat(entry.getId()).isNotNull();
        assertThat(cashFlowEntryRepository.entries).hasSize(1);
        assertThat(outboxRepository.events).hasSize(1);
        assertThat(outboxRepository.events.getFirst().getEventType()).isEqualTo("CashFlowEntryCreated");
    }

    @Test
    void shouldFailWhenAmountIsNotPositive() {
        CreateCashFlowEntryUseCase useCase = new CreateCashFlowEntryUseCase(
                new InMemoryCashFlowEntryRepository(),
                new InMemoryOutboxRepository(),
                new DomainEventJsonSerializer(new ObjectMapper().findAndRegisterModules()),
                Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(new CreateCashFlowEntryCommand(
                UUID.randomUUID(),
                "Venda",
                CashFlowEntryType.INCOME,
                BigDecimal.ZERO,
                "SALES",
                "MAIN",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 10),
                CashFlowEntryStatus.PAID
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFailWhenPaidStatusHasNoPaymentDate() {
        CreateCashFlowEntryUseCase useCase = new CreateCashFlowEntryUseCase(
                new InMemoryCashFlowEntryRepository(),
                new InMemoryOutboxRepository(),
                new DomainEventJsonSerializer(new ObjectMapper().findAndRegisterModules()),
                Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(new CreateCashFlowEntryCommand(
                UUID.randomUUID(),
                "Venda",
                CashFlowEntryType.INCOME,
                new BigDecimal("10.00"),
                "SALES",
                "MAIN",
                LocalDate.of(2026, 6, 10),
                null,
                CashFlowEntryStatus.PAID
        ))).isInstanceOf(BusinessException.class);
    }

    private CreateCashFlowEntryCommand validCommand() {
        return new CreateCashFlowEntryCommand(
                UUID.randomUUID(),
                "Venda",
                CashFlowEntryType.INCOME,
                new BigDecimal("10.00"),
                "SALES",
                "MAIN",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 10),
                CashFlowEntryStatus.PAID
        );
    }

    private static class InMemoryCashFlowEntryRepository implements CashFlowEntryRepository {

        private final List<CashFlowEntry> entries = new ArrayList<>();

        @Override
        public CashFlowEntry save(CashFlowEntry entry) {
            entries.add(entry);
            return entry;
        }

        @Override
        public List<CashFlowEntry> findByCompanyIdAndMonth(java.util.UUID companyId, java.time.YearMonth month) {
            return entries;
        }

        @Override
        public Optional<CashFlowEntry> findById(UUID id) {
            return entries.stream().filter(entry -> entry.getId().equals(id)).findFirst();
        }
    }

    private static class InMemoryOutboxRepository implements OutboxRepository {

        private final List<OutboxEvent> events = new ArrayList<>();

        @Override
        public OutboxEvent save(OutboxEvent event) {
            events.add(event);
            return event;
        }

        @Override
        public List<OutboxEvent> findPending(int limit) {
            return List.of();
        }

        @Override
        public List<OutboxEvent> findFailed(int limit) {
            return List.of();
        }

        @Override
        public void markAsPublished(UUID eventId) {
        }

        @Override
        public void markAsFailed(UUID eventId, String errorMessage) {
        }

        @Override
        public void incrementAttempts(UUID eventId, String errorMessage) {
        }

        @Override
        public OutboxStats getStats() {
            return new OutboxStats(0, 0, 0);
        }

        @Override
        public void resetFailedToPending() {
        }
    }
}
