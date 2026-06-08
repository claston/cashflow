package com.example.cashflow.cashflow.command.infrastructure;

import com.example.cashflow.cashflow.command.domain.CashFlowEntry;
import com.example.cashflow.cashflow.command.domain.CashFlowEntryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCashFlowEntryRepository implements CashFlowEntryRepository {

    private final SpringDataCashFlowEntryJpaRepository repository;

    public JpaCashFlowEntryRepository(SpringDataCashFlowEntryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public CashFlowEntry save(CashFlowEntry entry) {
        return repository.save(CashFlowEntryJpaEntity.fromDomain(entry)).toDomain();
    }

    @Override
    public List<CashFlowEntry> findByCompanyIdAndMonth(UUID companyId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        return repository.findByCompanyIdAndDueDateGreaterThanEqualAndDueDateLessThan(companyId, start, end)
                .stream()
                .map(CashFlowEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<CashFlowEntry> findById(UUID id) {
        return repository.findById(id).map(CashFlowEntryJpaEntity::toDomain);
    }
}
