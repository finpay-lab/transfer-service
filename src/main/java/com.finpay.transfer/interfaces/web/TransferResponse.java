package com.finpay.transfer.interfaces.web;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.time.Instant;
import java.util.UUID;

/** Transfer view (amount as decimal string). */
public record TransferResponse(
        UUID transferId,
        UUID customerId,
        UUID from,
        UUID to,
        String amount,
        String currency,
        TransferStatus status,
        SagaStep sagaStep,
        Instant createdAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.transferId(),
                transfer.customerId(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount().toPlainString(),
                transfer.currency(),
                transfer.status(),
                transfer.sagaStep(),
                transfer.createdAt());
    }
}