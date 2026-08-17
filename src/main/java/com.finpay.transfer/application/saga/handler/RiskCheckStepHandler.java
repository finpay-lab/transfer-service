package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/** RISK_CHECK step: reject the transfer when the risk decision is not APPROVE. */
@Component
public class RiskCheckStepHandler implements SagaStepHandler {

    private final RiskCheckPort riskCheckPort;

    public RiskCheckStepHandler(RiskCheckPort riskCheckPort) {
        this.riskCheckPort = riskCheckPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.RISK_CHECK;
    }

    @Override
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        if (!riskCheckPort.evaluate(transfer).isApproved()) {
            throw new SagaStepExecutionException(
                    SagaStep.RISK_CHECK, "Transfer rejected by risk evaluation");
        }
    }
}
