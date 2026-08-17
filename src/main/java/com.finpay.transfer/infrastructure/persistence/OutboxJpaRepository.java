package com.finpay.transfer.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link OutboxJpaEntity}. */
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {
}
