package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * FINALIZATION step: release the reservation and mark the transfer COMPLETED.
 *
 * <p>For this lab the release happens inside wallet-service as part of the
 * future HTTP reservation port; locally the step has no side effect — the
 * orchestrator records completion and publishes {@code TransferCompleted}.
 */
@Component
public class FinalizationStepHandler implements SagaStepHandler {

    @Override
    public SagaStep step() {
        return SagaStep.FINALIZATION;
    }

    @Override
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        // Completion is recorded by the orchestrator; nothing further to do.
    }
}
