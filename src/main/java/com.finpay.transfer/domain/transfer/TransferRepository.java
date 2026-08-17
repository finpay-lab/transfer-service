package com.finpay.transfer.domain.transfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for the transfer aggregate. Implementation lives in
 * infrastructure/ (AGENTS.md Rule 4); the domain only knows this interface.
 *
 * <p>Idempotency contract: {@link #save(Transfer)} must not create a second
 * transfer for an existing idempotency key. The implementation enforces this
 * with a unique constraint and surfaces a
 * {@link DuplicateIdempotencyKeyException} on a concurrent race.
 *
 * <p>Crash-recovery contract (ADR-0003): {@link #findNonTerminal(int)} returns
 * sagas that still need driving (status CREATED), ordered oldest-first by last
 * update. Re-driving them is safe because every step transition is idempotent
 * (keyed by transferId / step).
 */
public interface TransferRepository {

    Optional<Transfer> findById(UUID transferId);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findNonTerminal(int limit);

    Transfer save(Transfer transfer);
}
