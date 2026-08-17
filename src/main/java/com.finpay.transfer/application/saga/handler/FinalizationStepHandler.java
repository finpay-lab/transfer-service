package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.FundsReservationPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * FINALIZATION: closes the money flow. The reservation is released (consumed)
 * once both ledger legs are posted — the release is idempotent by
 * {@code reservationId}, so it is a no-op if a later compensation releases it
 * again. The terminal state ({@code COMPLETED}) and the
 * {@code TransferCompleted} event are applied by the orchestrator after this
 * step returns.
 */
@Component
public class FinalizationStepHandler implements SagaStepHandler {

    private final FundsReservationPort reservationPort;

    public FinalizationStepHandler(FundsReservationPort reservationPort) {
        this.reservationPort = reservationPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.FINALIZATION;
    }

    @Override
    public void execute(Transfer transfer) {
        UUID reservationId = transfer.execution().reservationId();
        if (reservationId == null) {
            return;
        }
        try {
            reservationPort.release(reservationId);
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to finalize reservation " + reservationId + " for transfer "
                            + transfer.transferId(), e);
        }
    }

    @Override
    public void compensate(Transfer transfer) {
        // No additional side effect: the reservation release is idempotent.
    }
}