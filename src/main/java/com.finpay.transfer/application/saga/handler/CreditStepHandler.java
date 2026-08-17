package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * CREDIT: posts the credit leg on the destination account (ledger). A failure
 * here (e.g. destination closed) triggers full compensation: the debit is
 * reversed and the reservation released. The reversing credit posting is
 * idempotent by {@code (transferId, leg)}.
 */
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
    public void execute(Transfer transfer) {
        try {
            ledgerPostingPort.credit(
                    transfer.transferId(),
                    transfer.destinationAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to credit destination account for transfer " + transfer.transferId(), e);
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        try {
            ledgerPostingPort.reverseCredit(
                    transfer.transferId(),
                    transfer.destinationAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to reverse credit for transfer " + transfer.transferId(), e);
        }
    }
}