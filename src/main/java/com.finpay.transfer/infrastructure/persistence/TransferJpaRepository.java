package com.finpay.transfer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    Optional<TransferJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
