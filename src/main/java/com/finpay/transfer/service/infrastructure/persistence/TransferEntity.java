package com.finpay.transfer.service.infrastructure.persistence;

import com.finpay.transfer.service.domain.Transfer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    private String transferId;
    private String idempotencyKey;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String currentStep;
    private boolean failed;
    private String failureReason;
    private Instant createdAt;

    public TransferEntity() {}

    public static TransferEntity from(Transfer t) {
        TransferEntity e = new TransferEntity();
        e.transferId = t.transferId();
        e.idempotencyKey = t.idempotencyKey();
        e.fromAccount = t.fromAccount();
        e.toAccount = t.toAccount();
        e.amount = t.amount();
        e.currency = t.currency();
        e.currentStep = t.currentStep().name();
        e.failed = t.isFailed();
        e.failureReason = t.failureReason();
        e.createdAt = t.createdAt();
        return e;
    }

    public Transfer toDomain() {
        Transfer t = new Transfer(transferId, idempotencyKey, fromAccount, toAccount, amount, currency);
        t.resumeAt(Transfer.Step.valueOf(currentStep));
        if (failed) t.fail(failureReason == null ? "recovered-failure" : failureReason);
        return t;
    }

    public String getTransferId() { return transferId; }
    public void setTransferId(String v) { this.transferId = v; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String v) { this.fromAccount = v; }
    public String getToAccount() { return toAccount; }
    public void setToAccount(String v) { this.toAccount = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String v) { this.currentStep = v; }
    public boolean isFailed() { return failed; }
    public void setFailed(boolean v) { this.failed = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
