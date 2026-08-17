package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/** Raised when a status transition is not in the legal state machine (Rule 9). */
public final class IllegalTransferStateTransitionException extends TransferDomainException {

    public IllegalTransferStateTransitionException(TransferStatus from, TransferStatus to) {
        super("Illegal transfer state transition " + from + " -> " + to);
    }
}
