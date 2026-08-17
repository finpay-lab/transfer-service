package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Raised when the VALIDATION step rejects a transfer because an upstream
 * business rule failed (customer not active, account not open, limit
 * exceeded). Distinct from infrastructure failures so the HTTP clients can
 * distinguish a definitive business rejection from a transient transport
 * error (which is retried).
 */
public final class TransferValidationException extends TransferDomainException {

    public TransferValidationException(String message) {
        super(message);
    }
}
