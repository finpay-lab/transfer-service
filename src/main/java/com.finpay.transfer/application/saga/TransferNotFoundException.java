package com.finpay.transfer.application.saga;

import java.util.UUID;

/** Raised when the saga coordinator cannot find the transfer to drive. */
public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(UUID transferId) {
        super("Transfer " + transferId + " not found");
    }
}