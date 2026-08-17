package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ledger posting step dependency (ledger-service: immutable postings,
 * invariant SUM(debits)==SUM(credits); transaction-flows.md). Postings are
 * keyed by {@code (transferId, leg)} so re-executing a step after a crash
 * cannot double-post (idempotent by transferId).
 */
public interface LedgerPostingPort {

    void debit(UUID transferId, UUID accountId, BigDecimal amount, String currency);

    void credit(UUID transferId, UUID accountId, BigDecimal amount, String currency);
}
