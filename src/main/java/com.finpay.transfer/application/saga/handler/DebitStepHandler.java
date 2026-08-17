package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/** DEBIT step: immutable ledger posting, source -amount. */
@Component
public class DebitStepHandler implements SagaStepHandler {

    private final LedgerPostingPort ledgerPostingPort;

    public DebitStepHandler(LedgerPostingPort ledgerPostingPort) {
        this.ledgerPostingPort = ledgerPostingPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.DEBIT;
    }

    @Override
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        try {
            ledgerPostingPort.debit(
                    transfer.transferId(),
                    transfer.sourceAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    SagaStep.DEBIT, "Source account debit failed", e);
        }
    }
}
