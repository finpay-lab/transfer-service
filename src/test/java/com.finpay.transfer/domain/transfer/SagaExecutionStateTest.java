package com.finpay.transfer.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SagaExecutionStateTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    @Test
    void initial_stateStartsAtValidation() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        assertThat(state.sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(state.executedSteps()).isEmpty();
        assertThat(state.compensatedSteps()).isEmpty();
        assertThat(state.isCompensating()).isFalse();
        assertThat(state.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void recordExecuted_advancesToNextMoneyFlowStep() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        state.recordExecuted(SagaStep.VALIDATION, NOW);
        state.recordExecuted(SagaStep.RESERVATION, NOW);

        assertThat(state.executedSteps()).containsExactlyInAnyOrder(SagaStep.VALIDATION, SagaStep.RESERVATION);
        assertThat(state.sagaStep()).isEqualTo(SagaStep.RISK_CHECK);
    }

    @Test
    void recordExecuted_finalizationHasNoNextStep() {
        SagaExecutionState state = SagaExecutionState.restore(
                SagaStep.CREDIT, Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK, SagaStep.DEBIT),
                Set.of(), false, null, null, null, NOW);

        state.recordExecuted(SagaStep.CREDIT, NOW);

        assertThat(state.sagaStep()).isEqualTo(SagaStep.FINALIZATION);
    }

    @Test
    void markCompensating_recordsReasonAndFailingStep() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        state.markCompensating("Destination account closed", SagaStep.CREDIT, NOW);

        assertThat(state.isCompensating()).isTrue();
        assertThat(state.sagaStep()).isEqualTo(SagaStep.COMPENSATION);
        assertThat(state.failureReason()).isEqualTo("Destination account closed");
        assertThat(state.failedAtStep()).isEqualTo(SagaStep.CREDIT);
    }

    @Test
    void nextStepToCompensate_returnsMostRecentExecutedStepFirst() {
        SagaExecutionState state = SagaExecutionState.restore(
                SagaStep.COMPENSATION,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK, SagaStep.DEBIT),
                Set.of(SagaStep.DEBIT), true, UUID.randomUUID(), "boom", SagaStep.CREDIT, NOW);

        Optional<SagaStep> next = state.nextStepToCompensate();

        assertThat(next).contains(SagaStep.RISK_CHECK);
        state.recordCompensated(SagaStep.RISK_CHECK, NOW);
        assertThat(state.nextStepToCompensate()).contains(SagaStep.RESERVATION);
        state.recordCompensated(SagaStep.RESERVATION, NOW);
        assertThat(state.nextStepToCompensate()).contains(SagaStep.VALIDATION);
    }

    @Test
    void nextStepToCompensate_emptyWhenAllCompensated() {
        SagaExecutionState state = SagaExecutionState.restore(
                SagaStep.COMPENSATION,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION),
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION),
                true, null, "boom", SagaStep.RISK_CHECK, NOW);

        assertThat(state.nextStepToCompensate()).isEmpty();
    }

    @Test
    void restore_rehydratesEveryField() {
        UUID reservationId = UUID.randomUUID();
        SagaExecutionState state = SagaExecutionState.restore(
                SagaStep.COMPENSATION,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION),
                Set.of(SagaStep.VALIDATION),
                true, reservationId, "boom", SagaStep.DEBIT, NOW);

        assertThat(state.sagaStep()).isEqualTo(SagaStep.COMPENSATION);
        assertThat(state.executedSteps()).containsExactlyInAnyOrder(SagaStep.VALIDATION, SagaStep.RESERVATION);
        assertThat(state.compensatedSteps()).containsExactly(SagaStep.VALIDATION);
        assertThat(state.isCompensating()).isTrue();
        assertThat(state.reservationId()).isEqualTo(reservationId);
        assertThat(state.failureReason()).isEqualTo("boom");
        assertThat(state.failedAtStep()).isEqualTo(SagaStep.DEBIT);
        assertThat(state.updatedAt()).isEqualTo(NOW);
    }
}
