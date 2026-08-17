package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

/**
 * Executes and compensates one saga step (ADR-0003). A handler is the seam
 * where the orchestrator issues a command to an external service.
 *
 * <p>Contracts:
 * <ul>
 *   <li>{@link #execute} must be idempotent — it may be re-invoked after a
 *       crash without double side effects (the underlying ports are idempotent
 *       by transferId / step).</li>
 *   <li>On a business/remote failure, {@link #execute} throws
 *       {@link SagaStepExecutionException}; the orchestrator then triggers
 *       compensation of already-executed steps.</li>
 *   <li>{@link #compensate} must be idempotent keyed by {@code (transferId,
 *       step)} — it may be retried after a crash mid-compensation.</li>
 *   <li>Steps without a side effect use a no-op {@link #compensate}.</li>
 * </ul>
 */
public interface SagaStepHandler {

    SagaStep step();

    void execute(Transfer transfer) throws SagaStepExecutionException;

    void compensate(Transfer transfer) throws SagaStepExecutionException;
}