package com.finpay.transfer.domain;

/** Base type for transfer domain exceptions. */
public class TransferDomainException extends RuntimeException {

    public TransferDomainException(String message) {
        super(message);
    }
}
