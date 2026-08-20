package com.finpay.transfer.service.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Transfer aggregate (FP-10/FP-11). Encapsulates the SAGA step state machine
 * and rejects illegal transitions (Rule 9). Persisted saga state enables
 * crash-recovery (FP-12): on restart the orchestrator resumes from the last
 * completed step.
 */
public class Transfer {

    public enum Step { INIT, VALIDATE, LIMIT, RISK, RESERVE, DEBIT, CREDIT, FINALIZE, COMPLETED, FAILED }

    // Legal forward transitions (Rule 9). Compensation may move to FAILED.
    private static final List<Step> ORDER =
            List.of(Step.INIT, Step.VALIDATE, Step.LIMIT, Step.RISK, Step.RESERVE, Step.DEBIT, Step.CREDIT, Step.FINALIZE, Step.COMPLETED);

    private final String transferId;
    private final String idempotencyKey;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal amount;
    private final String currency;
    private Step currentStep;
    private boolean failed;
    private String failureReason;
    private final Instant createdAt;

    public Transfer(String transferId, String idempotencyKey, String fromAccount,
                    String toAccount, BigDecimal amount, String currency) {
        this.transferId = transferId;
        this.idempotencyKey = idempotencyKey;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.currentStep = Step.INIT;
        this.createdAt = Instant.now();
    }

    public String transferId() { return transferId; }
    public String idempotencyKey() { return idempotencyKey; }
    public String fromAccount() { return fromAccount; }
    public String toAccount() { return toAccount; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public Step currentStep() { return currentStep; }
    public boolean isFailed() { return failed; }
    public String failureReason() { return failureReason; }
    public Instant createdAt() { return createdAt; }

    /** Advance to the next step. Throws on illegal transition (Rule 9). */
    public void advance() {
        if (failed) throw new IllegalStateException("transfer failed; cannot advance");
        int idx = ORDER.indexOf(currentStep);
        if (idx < 0 || idx + 1 >= ORDER.size()) {
            throw new IllegalStateTransition(currentStep, "next");
        }
        currentStep = ORDER.get(idx + 1);
    }

    /** Mark a compensating failure (Rule 9 allows -> FAILED). */
    public void fail(String reason) {
        this.failed = true;
        this.failureReason = reason;
        this.currentStep = Step.FAILED;
    }

    /** Resume-after-crash: set the step explicitly (must be a legal state). */
    public void resumeAt(Step step) {
        if (!ORDER.contains(step) && step != Step.FAILED) {
            throw new IllegalStateTransition(step, "resume");
        }
        this.currentStep = step;
    }

    public boolean isCompleted() {
        return currentStep == Step.COMPLETED && !failed;
    }

    public static final class IllegalStateTransition extends RuntimeException {
        IllegalStateTransition(Step from, String to) {
            super("illegal transition from " + from + " to " + to);
        }
    }
}
