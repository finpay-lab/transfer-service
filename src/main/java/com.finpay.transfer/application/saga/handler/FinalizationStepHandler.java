package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * FINALIZATION: closes the money flow. No external side effect here — the
 * orchestrator marks the transfer COMPLETED and publishes
 * {@code TransferCompleted}. Compensation is a no-op.
 */
@Component
public class FinalizationStepHandler implements SagaStepHandler {

    @Override
    public SagaStep step() {
        return SagaStep.FINALIZATION;
    }

    @Override
    public void execute(Transfer transfer) {
        // Nothing to do: terminal state + event are applied by the orchestrator.
    }

    @Override
    public void compensate(Transfer transfer) {
        // No external side effect to reverse.
    }
}
