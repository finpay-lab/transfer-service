package com.finpay.transfer.domain.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Transfer saga aggregate (transfer-service owns it, DATA_OWNERSHIP.md).
 *
 * <p>Plain Java on purpose: no Spring/JPA/Kafka types here (AGENTS.md Rule 4).
 * Money is {@link BigDecimal}, never floating point.
 *
 * <p>Idempotency (AGENTS.md Rule 6): a transfer is created with a
 * client-supplied idempotency key; at most one transfer may exist per key.
 */
public final class Transfer {

    private static final String CURRENCY_PATTERN = "[A-Z]{3}";

    private final UUID transferId;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;
    private final Instant createdAt;

    private TransferStatus status;
    private SagaStep sagaStep;

    private Transfer(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt,
            TransferStatus status,
            SagaStep sagaStep) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.status = status;
        this.sagaStep = sagaStep;
    }

    /** Creates a new transfer in its initial saga state (CREATED / VALIDATION). */
    public static Transfer create(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt) {
        validate(transferId, sourceAccountId, destinationAccountId, amount, currency, idempotencyKey, createdAt);
        return new Transfer(transferId, sourceAccountId, destinationAccountId, amount,
                currency, idempotencyKey, createdAt, TransferStatus.CREATED, SagaStep.VALIDATION);
    }

    /**
     * Rehydrates a transfer from persistence, preserving its current status
     * and saga step (unlike {@link #create}, which starts a fresh saga).
     */
    public static Transfer restore(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt,
            TransferStatus status,
            SagaStep sagaStep) {
        validate(transferId, sourceAccountId, destinationAccountId, amount, currency, idempotencyKey, createdAt);
        if (status == null || sagaStep == null) {
            throw new IllegalArgumentException("status and sagaStep must not be null");
        }
        return new Transfer(transferId, sourceAccountId, destinationAccountId, amount,
                currency, idempotencyKey, createdAt, status, sagaStep);
    }

    private static void validate(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId must not be null");
        }
        if (sourceAccountId == null || destinationAccountId == null) {
            throw new IllegalArgumentException("source and destination account must not be null");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be a positive number");
        }
        if (currency == null || !currency.matches(CURRENCY_PATTERN)) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO-4217 code");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }

    /** Rejects illegal transitions; only legal ones are applied (AGENTS.md Rule 9). */
    public void transitionTo(TransferStatus newStatus) {
        if (!legalTransitions().getOrDefault(status, Set.of()).contains(newStatus)) {
            throw new IllegalTransferStateTransitionException(status, newStatus);
        }
        status = newStatus;
    }

    /**
     * True when this transfer was created from the given request details. Used
     * by the idempotency check to distinguish a replay (same key, same
     * payload) from a conflict (same key, different payload).
     */
    public boolean matches(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency) {
        return this.sourceAccountId.equals(sourceAccountId)
                && this.destinationAccountId.equals(destinationAccountId)
                && this.amount.compareTo(amount) == 0
                && this.currency.equals(currency);
    }

    private static Map<TransferStatus, Set<TransferStatus>> legalTransitions() {
        return Map.of(
                TransferStatus.CREATED, Set.of(TransferStatus.COMPLETED, TransferStatus.FAILED, TransferStatus.REVERSED),
                TransferStatus.COMPLETED, Set.of(TransferStatus.REVERSED),
                TransferStatus.FAILED, Set.of(),
                TransferStatus.REVERSED, Set.of());
    }

    public UUID transferId() {
        return transferId;
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID destinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public TransferStatus status() {
        return status;
    }

    public SagaStep sagaStep() {
        return sagaStep;
    }
}
