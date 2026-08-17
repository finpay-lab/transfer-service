package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Raised when the database unique constraint on the idempotency key rejects a
 * concurrent duplicate insert. The DB constraint is the ultimate guard for the
 * idempotency invariant (at-most-one transfer per key); the loser of the race
 * should retry the request with the same key, which then replays the winner's
 * transfer (see CreateTransferUseCase).
 */
public class DuplicateIdempotencyKeyException extends TransferDomainException {

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("Concurrent duplicate request for idempotency key '" + idempotencyKey
                + "'; retry with the same key to get the original result");
    }
}
