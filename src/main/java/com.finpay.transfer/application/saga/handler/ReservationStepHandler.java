package com.finpay.transfer.application.saga.handler;

import com.finpay.transfer.application.saga.port.FundsReservationPort;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RESERVATION step: earmark funds on the source wallet (available→pending).
 * The reservationId is held by the reservation port keyed by transferId; it is
 * used by the future compensation path to release funds.
 */
@Component
public class ReservationStepHandler implements SagaStepHandler {

    private static final Logger log = LoggerFactory.getLogger(ReservationStepHandler.class);

    private final FundsReservationPort reservationPort;

    public ReservationStepHandler(FundsReservationPort reservationPort) {
        this.reservationPort = reservationPort;
    }

    @Override
    public SagaStep step() {
        return SagaStep.RESERVATION;
    }

    @Override
    public void execute(Transfer transfer) throws SagaStepExecutionException {
        try {
            reservationPort.reserve(
                    transfer.transferId(),
                    transfer.sourceAccountId(),
                    transfer.amount(),
                    transfer.currency());
        } catch (RuntimeException e) {
            throw new SagaStepExecutionException(
                    SagaStep.RESERVATION, "Funds reservation failed", e);
        }
    }
}
