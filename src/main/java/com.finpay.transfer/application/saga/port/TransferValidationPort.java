package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the VALIDATION step (SAGA command, ADR-0003): checks that the
 * customer is active, the source account is open, the destination account is
 * valid, and the amount is within the customer's limits.
 *
 * <p>Backed by customer-service, account-service and limit-service. In the lab
 * default it is an in-memory stand-in that always approves; the production
 * adapter (infrastructure/remote, {@code finpay.saga.remote-mode=http}) calls
 * the three services synchronously with timeout/retry (AGENTS.md Rule 8).
 */
public interface TransferValidationPort {

    /**
     * Validates a transfer attempt. Throws a {@code RuntimeException} carrying
     * a business reason when validation fails (e.g. customer inactive, account
     * closed, limit exceeded). Because VALIDATION is the first step, a failure
     * here has nothing to compensate.
     */
    void validate(ValidationRequest request);

    record ValidationRequest(
            UUID customerId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency) {}
}
