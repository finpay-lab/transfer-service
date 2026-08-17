package com.finpay.transfer.infrastructure.persistence;

import com.finpay.transfer.domain.transfer.DuplicateIdempotencyKeyException;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link TransferRepository}. The unique
 * constraint on {@code idempotency_key} is the ultimate guard for at-most-one
 * transfer per key: {@link #save(Transfer)} flushes eagerly so a concurrent
 * duplicate surfaces here as a {@link DataIntegrityViolationException} and is
 * translated to the domain {@link DuplicateIdempotencyKeyException}.
 */
@Repository
public class JpaTransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;

    public JpaTransferRepositoryAdapter(TransferJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Transfer save(Transfer transfer) {
        try {
            jpaRepository.saveAndFlush(toEntity(transfer));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdempotencyKeyException(transfer.idempotencyKey());
        }
        return transfer;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private TransferJpaEntity toEntity(Transfer transfer) {
        return new TransferJpaEntity(
                transfer.transferId(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount(),
                transfer.currency(),
                transfer.status(),
                transfer.sagaStep(),
                transfer.idempotencyKey(),
                transfer.createdAt());
    }

    private Transfer toDomain(TransferJpaEntity entity) {
        return Transfer.restore(
                entity.transferId(),
                entity.sourceAccountId(),
                entity.destinationAccountId(),
                entity.amount(),
                entity.currency(),
                entity.idempotencyKey(),
                entity.createdAt(),
                entity.status(),
                entity.sagaStep());
    }
}
