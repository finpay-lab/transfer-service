package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.RiskDecision;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the risk-service evaluation step. Always approves in
 * the lab; production would call risk-service synchronously (with
 * timeout/retry/circuit-breaker, AGENTS.md Rule 8).
 */
@Component
@ConditionalOnProperty(
        name = "finpay.saga.remote-mode",
        havingValue = "in-memory",
        matchIfMissing = true)
public class InMemoryRiskCheckService implements RiskCheckPort {

    @Override
    public RiskDecision evaluate(UUID transferId, UUID customerId, BigDecimal amount, String currency) {
        return RiskDecision.APPROVED;
    }
}