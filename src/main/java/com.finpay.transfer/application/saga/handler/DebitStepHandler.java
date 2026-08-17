package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.springframework.stereotype.Component;

/**
 * DEBIT: posts the debit leg on the source account (ledger). Compensation
 * posts a reversing credit; the ledger never edits a committed posting
 * (ADR-0003). Both postings are idempotent by {@code (transferId, leg)}.
 */
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
    public void execute(Transfer transfer) {
        try {
            ledgerPostingPort.debit(
                    transfer.transferId(),
                    transfer.sourceAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to debit source account for transfer " + transfer.transferId(), e);
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        try {
            ledgerPostingPort.reverseDebit(
                    transfer.transferId(),
                    transfer.sourceAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to reverse debit for transfer " + transfer.transferId(), e);
        }
    }
}