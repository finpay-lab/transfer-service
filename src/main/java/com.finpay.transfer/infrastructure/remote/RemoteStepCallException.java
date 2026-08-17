package com.finpay.transfer.infrastructure.remote;

/**
 * Raised when a remote dependency call ultimately fails after retries
 * (AGENTS.md Rule 8). Carries the operation name for observability; the saga
 * step handlers wrap it in a {@code SagaStepExecutionException} so the
 * orchestrator can decide between fail-fast and compensation.
 */
public final class RemoteStepCallException extends RuntimeException {

    public RemoteStepCallException(String operation, Throwable cause) {
        super("Remote call failed after retries: " + operation, cause);
    }
}
