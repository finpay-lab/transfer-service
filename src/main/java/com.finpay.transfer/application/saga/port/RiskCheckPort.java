package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the risk-service synchronous evaluation step (SAGA command,
 * ADR-0003). Evaluation is a pure read (no side effect to compensate).
 */
public interface RiskCheckPort {

    RiskDecision evaluate(UUID transferId, BigDecimal amount, String currency);
}
