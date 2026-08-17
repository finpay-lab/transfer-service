package com.finpay.transfer.domain.transfer;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persisted saga execution state (ADR-0003). Transfer-service is the SAGA
 * orchestrator: this object is what allows deterministic recovery after a
 * crash — the whole workflow state lives in the database, never in memory.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@link #sagaStep} — the step the saga is currently driving (starts at
 *       {@code VALIDATION}).</li>
 *   <li>{@link #executedSteps} — steps whose forward action completed; these
 *       must be compensated in reverse order if the saga later fails
 *       (compensation is a later concern, keyed by {@code (transferId, step)}).</li>
 *   <li>{@link #failureReason}/{@link #failedAtStep} — why and where the saga
 *       failed, surfaced on the {@code TransferFailed} event.</li>
 *   <li>{@link #updatedAt} — last state transition time; recovery scans
 *       non-terminal transfers oldest-first.</li>
 * </ul>
 */
public final class SagaExecutionState {

    private SagaStep sagaStep;
    private final Set<SagaStep> executedSteps;
    private String failureReason;
    private SagaStep failedAtStep;
    private Instant updatedAt;

    private SagaExecutionState(
            SagaStep sagaStep,
            Set<SagaStep> executedSteps,
            String failureReason,
            SagaStep failedAtStep,
            Instant updatedAt) {
        this.sagaStep = sagaStep;
        this.executedSteps = new HashSet<>(executedSteps);
        this.failureReason = failureReason;
        this.failedAtStep = failedAtStep;
        this.updatedAt = updatedAt;
    }

    /** Fresh saga: not started beyond the initial VALIDATION step. */
    public static SagaExecutionState initial(Instant createdAt) {
        return new SagaExecutionState(
                SagaStep.VALIDATION, Set.of(), null, null, createdAt);
    }

    /** Rehydrates persisted saga state (used by the repository adapter). */
    public static SagaExecutionState restore(
            SagaStep sagaStep,
            Set<SagaStep> executedSteps,
            String failureReason,
            SagaStep failedAtStep,
            Instant updatedAt) {
        return new SagaExecutionState(
                sagaStep, executedSteps, failureReason, failedAtStep, updatedAt);
    }

    /**
     * Records that a forward step completed and advances to the next step of
     * the money flow. Idempotent: re-persisting an already executed step is a
     * no-op (resume-after-crash safety).
     */
    public void recordExecuted(SagaStep step, Instant now) {
        executedSteps.add(step);
        sagaStep = step.next().orElse(sagaStep);
        updatedAt = now;
    }

    /**
     * Records that the saga failed at a step. The failing step itself is
     * <em>not</em> added to {@link #executedSteps}: only steps that already
     * completed may need compensation.
     */
    public void markFailed(String reason, SagaStep failedAt, Instant now) {
        this.failureReason = reason;
        this.failedAtStep = failedAt;
        this.updatedAt = now;
    }

    public SagaStep sagaStep() {
        return sagaStep;
    }

    public Set<SagaStep> executedSteps() {
        return Collections.unmodifiableSet(executedSteps);
    }

    public String failureReason() {
        return failureReason;
    }

    public SagaStep failedAtStep() {
        return failedAtStep;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
