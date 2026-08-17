package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.TransferValidationPort;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the VALIDATION step (customer/account/limit checks).
 * Always approves in the lab; production uses the HTTP adapter
 * (infrastructure/remote) which calls customer-service, account-service and
 * limit-service synchronously with timeout/retry/circuit-breaker (AGENTS.md
 * Rule 8).
 */
@Component
@ConditionalOnProperty(
        name = "finpay.saga.remote-mode",
        havingValue = "in-memory",
        matchIfMissing = true)
public class InMemoryTransferValidationService implements TransferValidationPort {

    @Override
    public void validate(ValidationRequest request) {
        // Lab stand-in: everything is assumed valid.
    }
}