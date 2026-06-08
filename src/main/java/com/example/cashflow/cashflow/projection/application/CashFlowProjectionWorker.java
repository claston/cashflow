package com.example.cashflow.cashflow.projection.application;

import com.example.cashflow.cashflow.command.domain.CashFlowEntryCreatedEvent;
import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import com.example.cashflow.cashflow.query.domain.CashFlowReadModelRepository;
import com.example.cashflow.diagnostics.application.ProjectionFailureModeService;
import com.example.cashflow.eventbus.domain.DomainEvent;
import com.example.cashflow.eventbus.domain.EventBus;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CashFlowProjectionWorker {

    private static final Logger log = LoggerFactory.getLogger(CashFlowProjectionWorker.class);

    private final EventBus eventBus;
    private final CashFlowReadModelRepository readModelRepository;
    private final ProjectionFailureModeService projectionFailureModeService;
    private final Clock clock;
    private final int maxEventsPerCycle;
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> processedEventIds = ConcurrentHashMap.newKeySet();

    public CashFlowProjectionWorker(
            EventBus eventBus,
            CashFlowReadModelRepository readModelRepository,
            ProjectionFailureModeService projectionFailureModeService,
            Clock clock,
            @Value("${app.projection.max-events-per-cycle:20}") int maxEventsPerCycle
    ) {
        this.eventBus = eventBus;
        this.readModelRepository = readModelRepository;
        this.projectionFailureModeService = projectionFailureModeService;
        this.clock = clock;
        this.maxEventsPerCycle = maxEventsPerCycle;
    }

    @Scheduled(fixedDelayString = "${app.projection.worker-delay-ms:1000}")
    public void processEvents() {
        for (int i = 0; i < maxEventsPerCycle; i++) {
            DomainEvent event = eventBus.poll().orElse(null);
            if (event == null) {
                return;
            }

            log.info("Processing domain event: eventId={}, eventType={}", event.eventId(), event.eventType());

            if (projectionFailureModeService.isEnabled()) {
                eventBus.publish(event);
                throw new IllegalStateException("Projection failure mode is enabled.");
            }

            if (processedEventIds.contains(event.eventId())) {
                continue;
            }

            if (event instanceof CashFlowEntryCreatedEvent cashFlowEntryCreatedEvent) {
                applyCashFlowEntryCreated(cashFlowEntryCreatedEvent);
                processedEventIds.add(event.eventId());
                continue;
            }

            log.warn("Ignoring unsupported domain event: eventId={}, eventType={}", event.eventId(), event.eventType());
            processedEventIds.add(event.eventId());
        }
    }

    private void applyCashFlowEntryCreated(CashFlowEntryCreatedEvent event) {
        Instant now = Instant.now(clock);
        YearMonth month = YearMonth.from(event.dueDate());
        CashFlowMonthlySummaryView summary = readModelRepository.findMonthlySummary(event.companyId(), month)
                .orElseGet(() -> CashFlowMonthlySummaryView.empty(event.companyId(), month, now));

        summary.apply(event, now);
        readModelRepository.upsert(summary);
        log.info("Monthly summary updated: companyId={}, month={}", event.companyId(), month);
    }
}
