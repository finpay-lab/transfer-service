package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

/**
 * Executes one saga step (ADR-0003). A handler is the seam where the
 * orchestrator issues a command to an external service through a port.
 *
 * <p>Contract: {@link #execute} must be idempotent — it may be re-invoked
 * after a crash without double side effects (the underlying ports are
 * idempotent by transferId / step). On a business/remote failure, execute
 * throws {@link SagaStepExecutionException}; the orchestrator then marks the
 * saga terminal-failed and persists the failure.
 */
public interface SagaStepHandler {

    SagaStep step();

    void execute(Transfer transfer) throws SagaStepExecutionException;
}
