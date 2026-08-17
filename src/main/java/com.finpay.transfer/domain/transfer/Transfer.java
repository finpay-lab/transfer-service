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
 *
 * <p>Crash recovery (ADR-0003): the whole saga state lives in
 * {@link SagaExecutionState} and is persisted on every step transition, so a
 * crash at any point can be resumed deterministically by re-driving the
 * persisted state.
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
    private final SagaExecutionState execution;

    private Transfer(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt,
            TransferStatus status,
            SagaExecutionState execution) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.status = status;
        this.execution = execution;
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
                currency, idempotencyKey, createdAt, TransferStatus.CREATED,
                SagaExecutionState.initial(createdAt));
    }

    /**
     * Rehydrates a transfer from persistence, preserving its current status
     * and saga state (unlike {@link #create}, which starts a fresh saga).
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
            SagaStep sagaStep,
            Set<SagaStep> executedSteps,
            Set<SagaStep> compensatedSteps,
            boolean compensating,
            UUID reservationId,
            String failureReason,
            SagaStep failedAtStep,
            Instant updatedAt) {
        validate(transferId, sourceAccountId, destinationAccountId, amount, currency, idempotencyKey, createdAt);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (sagaStep == null || updatedAt == null) {
            throw new IllegalArgumentException("sagaStep and updatedAt must not be null");
        }
        return new Transfer(transferId, sourceAccountId, destinationAccountId, amount,
                currency, idempotencyKey, createdAt, status,
                SagaExecutionState.restore(
                        sagaStep, executedSteps, compensatedSteps, compensating,
                        reservationId, failureReason, failedAtStep, updatedAt));
    }

    /** Backward-compatible restore without saga step state (fresh step log). */
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
        return restore(transferId, sourceAccountId, destinationAccountId, amount, currency,
                idempotencyKey, createdAt, status, sagaStep, Set.of(), Set.of(),
                false, null, null, null, createdAt);
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

    /** Marks the saga terminal-successful (all money-flow steps executed). */
    public void complete(Instant now) {
        transitionTo(TransferStatus.COMPLETED);
    }

    /** Marks the saga terminal-failed after compensation completed. */
    public void fail(Instant now) {
        transitionTo(TransferStatus.FAILED);
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

    public SagaExecutionState execution() {
        return execution;
    }

    public SagaStep sagaStep() {
        return execution.sagaStep();
    }

    /** True once the transfer reached a terminal status (COMPLETED / FAILED / REVERSED). */
    public boolean isTerminal() {
        return status != TransferStatus.CREATED;
    }
}
