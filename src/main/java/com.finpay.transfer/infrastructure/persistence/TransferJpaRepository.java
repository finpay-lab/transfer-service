package com.finpay.transfer.infrastructure.persistence;

import com.finpay.transfer.domain.transfer.TransferStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    Optional<TransferJpaEntity> findByIdempotencyKey(String idempotencyKey);

    /** Crash-recovery candidate query: all non-terminal sagas, oldest-first. */
    List<TransferJpaEntity> findByStatusOrderByUpdatedAtAsc(TransferStatus status);
}