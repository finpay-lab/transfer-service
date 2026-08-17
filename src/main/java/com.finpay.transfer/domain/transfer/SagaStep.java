package com.finpay.transfer.domain.transfer;

import java.util.Optional;

/**
 * Fine-grained steps of the orchestrated transfer saga (ADR-0003). Mirrors the
 * {@code sagaStep} values in the event contract
 * (contracts/events/v1/TransferCreated.json).
 *
 * <p>{@link #order()} gives the money-flow execution order and is used by
 * compensation to reverse executed steps in the opposite order (last step
 * executed is the first one compensated).
 *
 * <p>{@code NOTIFICATION} is a legal contract value but is <em>not</em> part
 * of the compensable money flow: ADR-0003 keeps stateless fan-out outside the
 * orchestrated (compensable) path. {@code COMPENSATION} marks the failure
 * state of a saga and is never a forward step.
 */
public enum SagaStep {
    VALIDATION(0),
    RESERVATION(1),
    RISK_CHECK(2),
    DEBIT(3),
    CREDIT(4),
    FINALIZATION(5),
    NOTIFICATION(6),
    COMPENSATION(-1);

    private final int order;

    SagaStep(int order) {
        this.order = order;
    }

    /** Execution order; higher means executed later (compensated earlier). */
    public int order() {
        return order;
    }

    /**
     * The next step of the money flow, or empty after {@code FINALIZATION}.
     * {@code NOTIFICATION} and {@code COMPENSATION} are not forward steps.
     */
    public Optional<SagaStep> next() {
        return switch (this) {
            case VALIDATION -> Optional.of(RESERVATION);
            case RESERVATION -> Optional.of(RISK_CHECK);
            case RISK_CHECK -> Optional.of(DEBIT);
            case DEBIT -> Optional.of(CREDIT);
            case CREDIT -> Optional.of(FINALIZATION);
            default -> Optional.empty();
        };
    }
}
