package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Raised when a saga step handler fails (business rejection or a remote
 * dependency error). The orchestrator turns this into a terminal failure
 * recorded in the persisted saga state (ADR-0003).
 */
public class SagaStepExecutionException extends TransferDomainException {

    private final SagaStep step;

    public SagaStepExecutionException(SagaStep step, String message) {
        super(message);
        this.step = step;
    }

    public SagaStepExecutionException(SagaStep step, String message, Throwable cause) {
        super(message + (cause == null ? "" : ": " + cause.getMessage()));
        this.step = step;
    }

    public SagaStep step() {
        return step;
    }
}
