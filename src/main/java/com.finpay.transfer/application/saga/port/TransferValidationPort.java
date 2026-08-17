package com.finpay.transfer.application.saga.port;

import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferValidationException;

/**
 * Validation step dependency (customer ACTIVE, account OPEN, beneficiary
 * valid, limit checks — transaction-flows.md). Remote implementation calls
 * customer/account/limit services with timeout/retry/circuit-breaker
 * (AGENTS.md Rule 8); the lab ships an in-memory stand-in.
 */
public interface TransferValidationPort {

    /** Throws {@link TransferValidationException} when validation fails. */
    void validate(Transfer transfer) throws TransferValidationException;
}
