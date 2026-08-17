package com.finpay.transfer.domain;

/**
 * Base class for domain-level errors; never leaks infrastructure exceptions.
 */
public abstract class TransferDomainException extends RuntimeException {

    protected TransferDomainException(String message) {
        super(message);
    }

    protected TransferDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
