package com.finpay.transfer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, String> {
    java.util.List<OutboxEntity> findBySentFalseOrderByCreatedAtAsc();
}
