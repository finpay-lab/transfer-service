package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/** Raised when the request payload fails domain-level validation. */
public class TransferValidationException extends TransferDomainException {

    public TransferValidationException(String message) {
        super(message);
    }
}
