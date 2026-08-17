package com.finpay.transfer.domain.transfer;

/**
 * Transfer lifecycle status (state machine, AGENTS.md Rule 9).
 *
 * <p>The saga executes fine-grained steps ({@link SagaStep}) while the
 * aggregate status stays {@code CREATED}; it only becomes terminal once the
 * saga succeeds ({@code COMPLETED}) or fails ({@code FAILED}). Legal
 * transitions are enforced by {@link Transfer#transitionTo(TransferStatus)};
 * illegal ones are rejected.
 */
public enum TransferStatus {
    CREATED,
    COMPLETED,
    FAILED,
    REVERSED
}
