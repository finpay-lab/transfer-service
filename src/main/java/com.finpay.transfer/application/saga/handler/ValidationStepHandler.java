package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.TransferValidationPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferValidationException;

import org.springframework.stereotype.Component;

/** VALIDATION step: customer ACTIVE, account OPEN, beneficiary valid, limits. */
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
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        try {
            validationPort.validate(transfer);
        } catch (TransferValidationException e) {
            throw new SagaStepExecutionException(SagaStep.VALIDATION, e.getMessage(), e);
        }
    }
}
