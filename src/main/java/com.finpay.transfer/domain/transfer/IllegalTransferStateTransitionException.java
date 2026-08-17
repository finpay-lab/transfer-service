package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/** Raised when a state machine transition is not legal (AGENTS.md Rule 9). */
public class IllegalTransferStateTransitionException extends TransferDomainException {

    public IllegalTransferStateTransitionException(TransferStatus from, TransferStatus to) {
        super("Illegal transfer state transition " + from + " -> " + to);
    }
}
