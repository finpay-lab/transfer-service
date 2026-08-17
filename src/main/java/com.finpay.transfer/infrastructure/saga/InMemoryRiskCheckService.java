package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.RiskDecision;
import com.finpay.transfer.domain.transfer.Transfer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the risk evaluation step. This is a lab seam: in
 * production this port is backed by risk-service (sync gRPC or async via
 * {@code RiskCheckCompleted}, with timeout/retry/circuit-breaker, AGENTS.md
 * Rule 8). Idempotent by transferId — a recovery redrive returns the cached
 * decision instead of double-evaluating.
 */
@Component
public class InMemoryRiskCheckService implements RiskCheckPort {

    private final Map<UUID, RiskDecision> decisionsByTransfer = new ConcurrentHashMap<>();
    private final Set<UUID> rejectedCustomers = ConcurrentHashMap.newKeySet();

    @Override
    public RiskDecision evaluate(Transfer transfer) {
        return decisionsByTransfer.computeIfAbsent(
                transfer.transferId(),
                id -> rejectedCustomers.contains(transfer.customerId())
                        ? new RiskDecision(RiskDecision.Decision.REJECT, 95, List.of("LAB_REJECT"))
                        : new RiskDecision(RiskDecision.Decision.APPROVE, 10, List.of("LAB_OK")));
    }

    /** Test hook: further evaluations for this customer reject the transfer. */
    public void rejectCustomer(UUID customerId) {
        rejectedCustomers.add(customerId);
    }

    /** Test hook: resets all in-memory risk decisions. */
    public void clearState() {
        decisionsByTransfer.clear();
        rejectedCustomers.clear();
    }
}