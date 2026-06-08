package com.example.cashflow.outbox.infrastructure;

import com.example.cashflow.outbox.domain.OutboxEventStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOutboxJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);

    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    long countByStatus(OutboxEventStatus status);
}
