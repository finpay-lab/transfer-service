package com.finpay.transfer.infrastructure.persistence;

import com.finpay.transfer.domain.transfer.TransferStatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link TransferJpaEntity}. */
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    /** Non-terminal sagas, oldest-first — the saga recovery scan set. */
    List<TransferJpaEntity> findByStatusOrderByUpdatedAtAsc(TransferStatus status);
}
