package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.TransferValidationPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * VALIDATION: local business validation plus the upstream checks
 * (customer active, source account open, destination valid, limits) via
 * {@link TransferValidationPort}. As the first step, nothing precedes it, so
 * its compensation is a no-op.
 */
@Component
public class ValidationStepHandler implements SagaStepHandler {

    private final TransferValidationPort validationPort;

    public ValidationStepHandler(TransferValidationPort validationPort) {
        this.validationPort = validationPort;
    }

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
        try {
            validationPort.validate(new TransferValidationPort.ValidationRequest(
                    transfer.customerId(),
                    transfer.sourceAccountId(),
                    transfer.destinationAccountId(),
                    transfer.amount(),
                    transfer.currency()));
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Validation failed for transfer " + transfer.transferId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        // No external side effect to reverse.
    }
}