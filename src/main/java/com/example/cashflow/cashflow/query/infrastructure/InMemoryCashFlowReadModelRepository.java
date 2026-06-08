package com.example.cashflow.cashflow.query.infrastructure;

import com.example.cashflow.cashflow.query.domain.CashFlowMonthlySummaryView;
import com.example.cashflow.cashflow.query.domain.CashFlowReadModelRepository;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCashFlowReadModelRepository implements CashFlowReadModelRepository {

    private final ConcurrentHashMap<String, CashFlowMonthlySummaryView> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<CashFlowMonthlySummaryView> findMonthlySummary(UUID companyId, YearMonth month) {
        CashFlowMonthlySummaryView view = storage.get(key(companyId, month));
        return Optional.ofNullable(view).map(CashFlowMonthlySummaryView::copyOf);
    }

    @Override
    public void upsert(CashFlowMonthlySummaryView summary) {
        storage.put(key(summary.getCompanyId(), summary.getMonth()), CashFlowMonthlySummaryView.copyOf(summary));
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public Map<String, CashFlowMonthlySummaryView> findAll() {
        return storage.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> CashFlowMonthlySummaryView.copyOf(entry.getValue())
                ));
    }

    private String key(UUID companyId, YearMonth month) {
        return companyId + ":" + month;
    }
}
