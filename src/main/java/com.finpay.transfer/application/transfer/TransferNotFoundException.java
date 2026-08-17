package com.finpay.transfer.application.transfer;

import java.util.UUID;

/** Raised when a transfer cannot be found (404 for the read path). */
public class TransferNotFoundException extends RuntimeException {

    private final UUID transferId;

    public TransferNotFoundException(UUID transferId) {
        super("Transfer " + transferId + " not found");
        this.transferId = transferId;
    }

    public UUID transferId() {
        return transferId;
    }
}
