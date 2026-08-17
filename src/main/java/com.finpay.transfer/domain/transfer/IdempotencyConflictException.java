package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Raised when a client reuses an idempotency key for a <em>different</em>
 * transfer request (AGENTS.md Rule 6). The caller must pick a new key.
 */
public class IdempotencyConflictException extends TransferDomainException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' was already used for a different transfer request");
    }
}
