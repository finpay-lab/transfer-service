package com.finpay.transfer.domain.transfer;

/**
 * Fine-grained steps of the orchestrated transfer saga (ADR-0003). Mirrors the
 * {@code sagaStep} values in the event contract
 * (contracts/events/v1/TransferCreated.json).
 */
public enum SagaStep {
    VALIDATION,
    RESERVATION,
    RISK_CHECK,
    DEBIT,
    CREDIT,
    FINALIZATION,
    NOTIFICATION,
    COMPENSATION
}
