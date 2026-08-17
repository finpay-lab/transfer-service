package com.finpay.transfer.domain.transfer;

import com.finpay.transfer.domain.TransferDomainException;

/**
 * Raised when a state-machine transition is requested that the transfer does
 * not allow (AGENTS.md Rule 9: every state machine defines legal transitions;
 * reject invalid ones).
 */
public class IllegalTransferStateTransitionException extends TransferDomainException {

    private final TransferStatus from;
    private final TransferStatus to;

    public IllegalTransferStateTransitionException(TransferStatus from, TransferStatus to) {
        super("Illegal transfer state transition " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public TransferStatus from() {
        return from;
    }

    public TransferStatus to() {
        return to;
    }
}
