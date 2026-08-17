package com.finpay.transfer.infrastructure.remote;

import com.finpay.transfer.application.saga.port.LedgerPostingPort;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for the ledger-service debit/credit steps. Each leg is posted
 * idempotently by {@code (transferId, leg)}: ledger-service asserts
 * {@code SUM(debits) == SUM(credits)} per transfer within one transaction and
 * ignores a re-posted leg. Reversing entries are new immutable postings with
 * their own leg (ADR-0003), never edits.
 */
public class HttpLedgerPostingClient extends RemoteClientSupport implements LedgerPostingPort {

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String LEG_REVERSE_DEBIT = "REVERSE_DEBIT";
    private static final String LEG_REVERSE_CREDIT = "REVERSE_CREDIT";

    private final RestClient ledgerClient;

    public HttpLedgerPostingClient(RestClient ledgerClient, RemoteClientsProperties properties) {
        super(properties.getMaxAttempts(), properties.getBackoffMillis());
        this.ledgerClient = ledgerClient;
    }

    @Override
    public void debit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, LEG_DEBIT, amount, currency);
    }

    @Override
    public void credit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, LEG_CREDIT, amount, currency);
    }

    @Override
    public void reverseDebit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, LEG_REVERSE_DEBIT, amount, currency);
    }

    @Override
    public void reverseCredit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        post(transferId, accountId, LEG_REVERSE_CREDIT, amount, currency);
    }

    private void post(UUID transferId, UUID accountId, String leg, BigDecimal amount, String currency) {
        callWithRetry("ledger posting " + transferId + ":" + leg, () -> {
            ledgerClient.post()
                    .uri("/v1/ledger/postings")
                    .body(new PostingRequest(
                            transferId.toString(),
                            accountId.toString(),
                            leg,
                            amount.toPlainString(),
                            currency))
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    public record PostingRequest(String transferId, String accountId, String leg, String amount, String currency) {}
}