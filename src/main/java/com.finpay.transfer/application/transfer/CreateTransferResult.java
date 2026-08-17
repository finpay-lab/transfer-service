package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.transfer.Transfer;

/**
 * Outcome of an idempotent creation. {@code created == true} means the
 * transfer was newly persisted; {@code false} means the request was a replay
 * of an existing transfer with the same idempotency key.
 */
public record CreateTransferResult(Transfer transfer, boolean created) {

    public static CreateTransferResult created(Transfer transfer) {
        return new CreateTransferResult(transfer, true);
    }

    public static CreateTransferResult replayed(Transfer transfer) {
        return new CreateTransferResult(transfer, false);
    }
}