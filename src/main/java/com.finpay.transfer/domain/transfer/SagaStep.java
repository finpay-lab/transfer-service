package com.finpay.transfer.domain.transfer;

import java.util.Optional;

/**
 * Fine-grained steps of the orchestrated transfer saga (ADR-0003). Mirrors the
 * {@code sagaStep} values in the event contract
 * (contracts/events/v1/TransferCreated.json): {@code VALIDATION},
 * {@code RISK_CHECK}, {@code RESERVATION}, {@code DEBIT}, {@code CREDIT},
 * {@code FINALIZATION}, {@code NOTIFICATION}, {@code COMPENSATION}.
 *
 * <p>{@link #order()} gives the money-flow execution order and is used by
 * compensation (a later concern) to reverse executed steps in the opposite
 * order.
 *
 * <p>The forward money flow is
 * {@code VALIDATION → RISK_CHECK → RESERVATION → DEBIT → CREDIT → FINALIZATION}
 * (architecture/transaction-flows.md): the VALIDATION step covers the local
 * check plus the customer/account/limit calls, then risk is evaluated before
 * funds are earmarked so a rejection needs no reservation to unwind.
 *
 * <p>{@code NOTIFICATION} is a legal contract value but is <em>not</em> part
 * of the compensable money flow: ADR-0003 keeps stateless fan-out outside the
 * orchestrated (compensable) path. {@code COMPENSATION} marks the failure
 * state of a saga and is never a forward step.
 */
public enum SagaStep {
    VALIDATION(0),
    RISK_CHECK(1),
    RESERVATION(2),
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
            case VALIDATION -> Optional.of(RISK_CHECK);
            case RISK_CHECK -> Optional.of(RESERVATION);
            case RESERVATION -> Optional.of(DEBIT);
            case DEBIT -> Optional.of(CREDIT);
            case CREDIT -> Optional.of(FINALIZATION);
            default -> Optional.empty();
        };
    }
}
