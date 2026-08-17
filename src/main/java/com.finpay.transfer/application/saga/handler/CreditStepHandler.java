package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/** CREDIT step: immutable ledger posting, destination +amount. */
@Component
public class CreditStepHandler implements SagaStepHandler {

    private final LedgerPostingPort ledgerPostingPort;

    public CreditStepHandler(LedgerPostingPort ledgerPostingPort) {
        this.ledgerPostingPort = ledgerPostingPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.CREDIT;
    }

    @Override
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        try {
            ledgerPostingPort.credit(
                    transfer.transferId(),
                    transfer.destinationAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    SagaStep.CREDIT, "Destination account credit failed", e);
        }
    }
}
