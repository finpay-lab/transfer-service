package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the ledger-service debit/credit steps. This is a lab
 * seam: in production this port is backed by ledger-service (with
 * timeout/retry/circuit-breaker, AGENTS.md Rule 8).
 *
 * <p>Postings are immutable and keyed by {@code (transferId, leg)} (ADR-0003:
 * reversing entries are new immutable postings, never edits), which makes every
 * operation idempotent — a crash-recovery redrive or a compensation retry
 * cannot double-post.
 *
 * <p>{@link #markAccountClosed(UUID)} lets a test simulate a business failure
 * at the CREDIT step (e.g. destination closed), which triggers full saga
 * compensation.
 */
@Component
@ConditionalOnProperty(
        name = "finpay.saga.remote-mode",
        havingValue = "in-memory",
        matchIfMissing = true)
public class InMemoryLedgerService implements LedgerPostingPort {

    private final Map<PostingKey, Posting> postings = new ConcurrentHashMap<>();
    private final Set<UUID> closedAccounts = ConcurrentHashMap.newKeySet();

    public record Posting(
            UUID transferId,
            UUID accountId,
            String leg,
            BigDecimal amount,
            String currency) {}

    private record PostingKey(UUID transferId, String leg) {}

    @Override
    public void debit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, "DEBIT", amount, currency);
    }

    @Override
    public void credit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        if (closedAccounts.contains(accountId)) {
            throw new IllegalStateException(
                    "Destination account " + accountId + " is closed");
        }
        post(transferId, accountId, "CREDIT", amount, currency);
    }

    @Override
    public void reverseDebit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, "REVERSE_DEBIT", amount, currency);
    }

    @Override
    public void reverseCredit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, "REVERSE_CREDIT", amount, currency);
    }

    private void post(UUID transferId, UUID accountId, String leg, BigDecimal amount, String currency) {
        postings.computeIfAbsent(
                new PostingKey(transferId, leg),
                key -> new Posting(transferId, accountId, leg, amount, currency));
    }

    /** Test hook: further credits to this account fail (business failure). */
    public void markAccountClosed(UUID accountId) {
        closedAccounts.add(accountId);
    }

    public boolean hasPosting(UUID transferId, String leg) {
        return postings.containsKey(new PostingKey(transferId, leg));
    }

    /** Test hook: resets all in-memory postings and closed accounts. */
    public void clearState() {
        postings.clear();
        closedAccounts.clear();
    }
}