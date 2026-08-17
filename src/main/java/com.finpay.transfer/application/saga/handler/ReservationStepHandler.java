package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.FundsReservationPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * RESERVATION: reserves funds on the source wallet. Compensation releases the
 * reservation. Both the reservation (idempotent by transferId) and the release
 * (idempotent by reservationId) are safe to retry after a crash.
 */
@Component
public class ReservationStepHandler implements SagaStepHandler {

    private final FundsReservationPort reservationPort;

    public ReservationStepHandler(FundsReservationPort reservationPort) {
        this.reservationPort = reservationPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.RESERVATION;
    }

    @Override
    public void execute(Transfer transfer) {
        UUID reservationId;
        try {
            reservationId = reservationPort.reserve(
                    transfer.transferId(),
                    transfer.sourceAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to reserve funds for transfer " + transfer.transferId(), e);
        }
        transfer.execution().setReservationId(reservationId);
    }

    @Override
    public void compensate(Transfer transfer) {
        UUID reservationId = transfer.execution().reservationId();
        if (reservationId == null) {
            return;
        }
        try {
            reservationPort.release(reservationId);
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    "Failed to release reservation " + reservationId + " for transfer "
                            + transfer.transferId(), e);
        }
    }
}
