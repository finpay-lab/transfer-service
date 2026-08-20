package com.finpay.transfer.service.infrastructure.persistence;

import com.finpay.transfer.service.domain.Transfer;
import com.finpay.transfer.service.domain.TransferRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class TransferRepositoryImpl implements TransferRepository {

    private final TransferJpaRepository transfers;
    private final IdempotencyJpaRepository idempotency;

    public TransferRepositoryImpl(TransferJpaRepository transfers, IdempotencyJpaRepository idempotency) {
        this.transfers = transfers;
        this.idempotency = idempotency;
    }

    @Override
    public Optional<Transfer> find(String transferId) {
        return transfers.findById(transferId).map(TransferEntity::toDomain);
    }

    @Override
    public Transfer save(Transfer transfer) {
        return transfers.save(TransferEntity.from(transfer)).toDomain();
    }

    @Override
    public boolean idempotencyExists(String key) {
        return idempotency.existsById(key);
    }

    @Override
    public void markIdempotent(String key, String transferId) {
        idempotency.save(new IdempotencyEntity(key, transferId));
    }
}
