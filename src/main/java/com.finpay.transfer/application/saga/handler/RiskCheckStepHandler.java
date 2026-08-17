package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.RiskDecision;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * RISK_CHECK: synchronous risk evaluation. A rejection fails the step and
 * triggers compensation of the reservation. Evaluation is a read, so the
 * compensation is a no-op.
 */
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
    public void execute(Transfer transfer) {
        RiskDecision decision;
        try {
            decision = riskCheckPort.evaluate(
                    transfer.transferId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Risk evaluation failed for transfer " + transfer.transferId(), e);
        }
        if (decision == RiskDecision.REJECTED) {
            throw new SagaStepExecutionException(
                    "Risk evaluation rejected transfer " + transfer.transferId());
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        // Evaluation is a read; nothing to reverse.
    }
}
