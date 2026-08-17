package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.transfer.Transfer;

/** Outcome of {@link CreateTransferUseCase}: the created transfer. */
public record CreateTransferResult(Transfer transfer) {

    public static CreateTransferResult of(Transfer transfer) {
        return new CreateTransferResult(transfer);
    }
}