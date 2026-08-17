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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "customer_id", columnDefinition = "uuid", nullable = false)
    private UUID customerId;

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

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "failed_at_step", length = 32)
    private SagaStep failedAtStep;

    /** JSONB-encoded set of executed saga steps (ADR-0003 persisted state). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "executed_steps", columnDefinition = "jsonb", nullable = false)
    private String executedSteps;

    protected TransferJpaEntity() {
        // JPA
    }

    public TransferJpaEntity(
            UUID transferId,
            UUID customerId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            TransferStatus status,
            SagaStep sagaStep,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt,
            String failureReason,
            SagaStep failedAtStep,
            String executedSteps) {
        this.transferId = transferId;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.sagaStep = sagaStep;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.failureReason = failureReason;
        this.failedAtStep = failedAtStep;
        this.executedSteps = executedSteps;
    }

    public UUID transferId() {
        return transferId;
    }

    public UUID customerId() {
        return customerId;
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

    public Instant updatedAt() {
        return updatedAt;
    }

    public String failureReason() {
        return failureReason;
    }

    public SagaStep failedAtStep() {
        return failedAtStep;
    }

    public String executedSteps() {
        return executedSteps;
    }
}
