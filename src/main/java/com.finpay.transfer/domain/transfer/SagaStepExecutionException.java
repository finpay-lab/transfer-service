package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Signals that a saga step could not be completed (business or remote
 * failure). The orchestrator catches this to trigger compensation (ADR-0003).
 * Compensators may also throw it; the orchestrator then leaves the saga in the
 * compensating state and the recovery job retries later (compensation is
 * idempotent, keyed by {@code (transferId, step)}).
 */
public class SagaStepExecutionException extends TransferDomainException {

    public SagaStepExecutionException(String message) {
        super(message);
    }

    public SagaStepExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
