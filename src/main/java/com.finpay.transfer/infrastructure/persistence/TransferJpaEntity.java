package com.finpay.transfer.infrastructure.persistence;

import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA mapping of the transfer aggregate (schema owned by this service). */
@Entity
@Table(
        name = "transfer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transfer_idempotency_key",
                columnNames = "idempotency_key"))
public class TransferJpaEntity {

    @Id
    @Column(name = "transfer_id", columnDefinition = "uuid", nullable = false)
    private UUID transferId;

    @Column(name = "source_account_id", columnDefinition = "uuid", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", columnDefinition = "uuid", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_step", nullable = false, length = 32)
    private SagaStep sagaStep;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransferJpaEntity() {
        // JPA
    }

    public TransferJpaEntity(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            TransferStatus status,
            SagaStep sagaStep,
            String idempotencyKey,
            Instant createdAt) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.sagaStep = sagaStep;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
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

    public TransferStatus status() {
        return status;
    }

    public SagaStep sagaStep() {
        return sagaStep;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
