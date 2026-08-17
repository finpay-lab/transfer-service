package com.finpay.transfer.application.saga.port;

import com.finpay.transfer.domain.transfer.Transfer;

/**
 * Risk evaluation step dependency (risk-service, async via RiskCheckCompleted
 * or sync; transaction-flows.md). Must be idempotent by transferId — a
 * crash-recovery redrive must not double-evaluate.
 */
public interface RiskCheckPort {

    RiskDecision evaluate(Transfer transfer);
}
