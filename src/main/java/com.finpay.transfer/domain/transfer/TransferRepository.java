package com.finpay.transfer.domain.transfer;

import java.util.Optional;

/**
 * Repository port for the transfer aggregate. Implementation lives in
 * infrastructure/ (AGENTS.md Rule 4); the domain only knows this interface.
 *
 * <p>Idempotency contract: {@link #save(Transfer)} must not create a second
 * transfer for an existing idempotency key. The implementation enforces this
 * with a unique constraint and surfaces a
 * {@link DuplicateIdempotencyKeyException} on a concurrent race.
 */
public interface TransferRepository {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    Transfer save(Transfer transfer);
}
