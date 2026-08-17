package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the ledger-service debit/credit steps (SAGA commands, ADR-0003).
 * Reversing entries are new immutable postings, never edits (ADR-0003).
 *
 * <p>All operations are idempotent by {@code (transferId, leg)} so that a
 * crash-recovery redrive cannot double-post a debit/credit, and a
 * compensation retry cannot double-reverse it.
 */
public interface LedgerPostingPort {

    void debit(UUID transferId, UUID accountId, BigDecimal amount, String currency);

    void credit(UUID transferId, UUID accountId, BigDecimal amount, String currency);

    /** Compensation for {@link #debit}: a reversing credit posting. */
    void reverseDebit(UUID transferId, UUID accountId, BigDecimal amount, String currency);

    /** Compensation for {@link #credit}: a reversing debit posting. */
    void reverseCredit(UUID transferId, UUID accountId, BigDecimal amount, String currency);
}
