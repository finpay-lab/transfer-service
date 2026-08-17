package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * VALIDATION: local business validation of the transfer request. Pure local
 * work (no remote call), so its compensation is a no-op.
 */
@Component
public class ValidationStepHandler implements SagaStepHandler {

    @Override
    public SagaStep step() {
        return SagaStep.VALIDATION;
    }

    @Override
    public void execute(Transfer transfer) {
        if (transfer.sourceAccountId().equals(transfer.destinationAccountId())) {
            throw new SagaStepExecutionException(
                    "Source and destination account must be different");
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        // No external side effect to reverse.
    }
}
