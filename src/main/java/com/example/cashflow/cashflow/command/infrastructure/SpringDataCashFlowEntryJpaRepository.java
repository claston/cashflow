package com.example.cashflow.cashflow.command.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCashFlowEntryJpaRepository extends JpaRepository<CashFlowEntryJpaEntity, UUID> {

    List<CashFlowEntryJpaEntity> findByCompanyIdAndDueDateGreaterThanEqualAndDueDateLessThan(
            UUID companyId,
            LocalDate start,
            LocalDate end
    );
}
