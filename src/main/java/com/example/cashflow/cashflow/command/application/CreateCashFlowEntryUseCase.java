package com.example.cashflow.cashflow.command.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryCreatedEvent;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import com.example.cashflow.outbox.domain.OutboxEvent;
import com.example.cashflow.outbox.domain.OutboxRepository;
import com.example.cashflow.shared.json.DomainEventJsonSerializer;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCashFlowEntryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateCashFlowEntryUseCase.class);

    private final CashFlowEntryRepository cashFlowEntryRepository;
    private final OutboxRepository outboxRepository;
    private final DomainEventJsonSerializer domainEventJsonSerializer;
    private final Clock clock;

    public CreateCashFlowEntryUseCase(
            CashFlowEntryRepository cashFlowEntryRepository,
            OutboxRepository outboxRepository,
            DomainEventJsonSerializer domainEventJsonSerializer,
            Clock clock
    ) {
        this.cashFlowEntryRepository = cashFlowEntryRepository;
        this.outboxRepository = outboxRepository;
        this.domainEventJsonSerializer = domainEventJsonSerializer;
        this.clock = clock;
    }

    @Transactional
    public CashFlowEntry execute(CreateCashFlowEntryCommand command) {
        Instant now = Instant.now(clock);
        CashFlowEntry entry = CashFlowEntry.create(
                command.companyId(),
                command.description(),
                command.type(),
                command.amount(),
                command.category(),
                command.account(),
                command.dueDate(),
                command.paymentDate(),
                command.status(),
                now
        );

        CashFlowEntry savedEntry = cashFlowEntryRepository.save(entry);
        log.info(
                "CashFlow entry created: entryId={}, companyId={}, amount={}, type={}",
                savedEntry.getId(),
                savedEntry.getCompanyId(),
                savedEntry.getAmount(),
                savedEntry.getType()
        );

        CashFlowEntryCreatedEvent event = CashFlowEntryCreatedEvent.from(savedEntry);
        outboxRepository.save(OutboxEvent.createPending(
                savedEntry.getId(),
                "CashFlowEntry",
                event.eventType(),
                domainEventJsonSerializer.serialize(event),
                now
        ));
        log.info("Outbox event created: eventId={}, eventType={}", event.eventId(), event.eventType());

        return savedEntry;
    }
}
