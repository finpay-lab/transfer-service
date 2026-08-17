package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the risk-service synchronous evaluation step (SAGA command,
 * ADR-0003). Evaluation is a pure read (no side effect to compensate).
 *
 * <p>{@code transferId} doubles as the request idempotency key (Rule 6): a
 * replayed evaluation for the same transfer returns the stored decision
 * without re-running the rules.
 */
public interface RiskCheckPort {

    RiskDecision evaluate(UUID transferId, UUID customerId, BigDecimal amount, String currency);
}
