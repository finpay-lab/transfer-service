package com.finpay.transfer.domain.transfer;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persisted saga execution state (ADR-0003). Transfer-service is the SAGA
 * orchestrator: this object is what allows deterministic recovery after a
 * crash — the whole workflow state lives in the database, never in memory.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@link #sagaStep} — the step the saga is currently driving (starts at
 *       {@code VALIDATION}, becomes {@code COMPENSATION} during compensation).</li>
 *   <li>{@link #executedSteps} — steps whose forward action completed; these
 *       must be compensated in reverse order if the saga later fails.</li>
 *   <li>{@link #compensatedSteps} — steps already compensated. Compensation is
 *       idempotent, keyed by {@code (transferId, step)} (ADR-0003): a crash
 *       mid-compensation only re-runs the remaining steps.</li>
 *   <li>{@link #compensating} — true once the saga entered the compensation
 *       path; recovery resumes compensation instead of forward progress.</li>
 *   <li>{@link #reservationId} — the wallet reservation reference created by
 *       the RESERVATION step, used by the RELEASE compensation.</li>
 *   <li>{@link #failureReason}/{@link #failedAtStep} — why and where the saga
 *       failed, surfaced on the {@code TransferFailed} event.</li>
 * </ul>
 */
public final class SagaExecutionState {

    private SagaStep sagaStep;
    private final Set<SagaStep> executedSteps;
    private final Set<SagaStep> compensatedSteps;
    private boolean compensating;
    private UUID reservationId;
    private String failureReason;
    private SagaStep failedAtStep;
    private Instant updatedAt;

    private SagaExecutionState(
            SagaStep sagaStep,
            Set<SagaStep> executedSteps,
            Set<SagaStep> compensatedSteps,
            boolean compensating,
            UUID reservationId,
            String failureReason,
            SagaStep failedAtStep,
            Instant updatedAt) {
        this.sagaStep = sagaStep;
        this.executedSteps = new HashSet<>(executedSteps);
        this.compensatedSteps = new HashSet<>(compensatedSteps);
        this.compensating = compensating;
        this.reservationId = reservationId;
        this.failureReason = failureReason;
        this.failedAtStep = failedAtStep;
        this.updatedAt = updatedAt;
    }

    /** Fresh saga: not started beyond the initial VALIDATION step. */
    public static SagaExecutionState initial(Instant createdAt) {
        return new SagaExecutionState(
                SagaStep.VALIDATION,
                Set.of(),
                Set.of(),
                false,
                null,
                null,
                null,
                createdAt);
    }

    /** Rehydrates persisted saga state (used by the repository adapter). */
    public static SagaExecutionState restore(
            SagaStep sagaStep,
            Set<SagaStep> executedSteps,
            Set<SagaStep> compensatedSteps,
            boolean compensating,
            UUID reservationId,
            String failureReason,
            SagaStep failedAtStep,
            Instant updatedAt) {
        return new SagaExecutionState(
                sagaStep,
                executedSteps,
                compensatedSteps,
                compensating,
                reservationId,
                failureReason,
                failedAtStep,
                updatedAt);
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

    /** Records that a step has been compensated. Idempotent by step. */
    public void recordCompensated(SagaStep step, Instant now) {
        compensatedSteps.add(step);
        updatedAt = now;
    }

    /**
     * Enters the compensation path after a step failure (ADR-0003). The
     * failing step itself is <em>not</em> added to {@link #executedSteps}: only
     * steps that already completed must be compensated.
     */
    public void markCompensating(String reason, SagaStep failedAt, Instant now) {
        this.compensating = true;
        this.failureReason = reason;
        this.failedAtStep = failedAt;
        this.sagaStep = SagaStep.COMPENSATION;
        this.updatedAt = now;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    /**
     * The next step to compensate: the most recently executed step that has
     * not been compensated yet. Compensation runs in reverse execution order.
     */
    public Optional<SagaStep> nextStepToCompensate() {
        return executedSteps.stream()
                .filter(step -> !compensatedSteps.contains(step))
                .max(Comparator.comparingInt(SagaStep::order));
    }

    public boolean isCompensating() {
        return compensating;
    }

    public SagaStep sagaStep() {
        return sagaStep;
    }

    public Set<SagaStep> executedSteps() {
        return Collections.unmodifiableSet(executedSteps);
    }

    public Set<SagaStep> compensatedSteps() {
        return Collections.unmodifiableSet(compensatedSteps);
    }

    public UUID reservationId() {
        return reservationId;
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
