package com.finpay.transfer.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SagaExecutionStateTest {

    private static final Instant NOW = Instant.parse("2026-08-12T06:30:00Z");

    @Test
    void initial_state_starts_at_validation_with_no_executed_steps() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        assertThat(state.sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(state.executedSteps()).isEmpty();
        assertThat(state.failureReason()).isNull();
        assertThat(state.failedAtStep()).isNull();
        assertThat(state.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void record_executed_advances_to_the_next_forward_step() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        state.recordExecuted(SagaStep.VALIDATION, NOW.plusSeconds(1));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.RISK_CHECK);
        assertThat(state.executedSteps()).containsExactly(SagaStep.VALIDATION);

        state.recordExecuted(SagaStep.RISK_CHECK, NOW.plusSeconds(2));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.RESERVATION);

        state.recordExecuted(SagaStep.RESERVATION, NOW.plusSeconds(3));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.DEBIT);

        state.recordExecuted(SagaStep.DEBIT, NOW.plusSeconds(4));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.CREDIT);

        state.recordExecuted(SagaStep.CREDIT, NOW.plusSeconds(5));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.FINALIZATION);

        state.recordExecuted(SagaStep.FINALIZATION, NOW.plusSeconds(6));
        assertThat(state.sagaStep()).isEqualTo(SagaStep.FINALIZATION);
        assertThat(state.executedSteps()).hasSize(6);
    }

    @Test
    void record_executed_is_idempotent_for_an_already_executed_step() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);

        state.recordExecuted(SagaStep.VALIDATION, NOW.plusSeconds(1));
        state.recordExecuted(SagaStep.VALIDATION, NOW.plusSeconds(2));

        assertThat(state.executedSteps()).hasSize(1);
        assertThat(state.sagaStep()).isEqualTo(SagaStep.RISK_CHECK);
    }

    @Test
    void mark_failed_records_reason_and_step_without_adding_executed_step() {
        SagaExecutionState state = SagaExecutionState.initial(NOW);
        state.recordExecuted(SagaStep.VALIDATION, NOW.plusSeconds(1));

        state.markFailed("Destination account closed", SagaStep.CREDIT, NOW.plusSeconds(5));

        assertThat(state.failureReason()).isEqualTo("Destination account closed");
        assertThat(state.failedAtStep()).isEqualTo(SagaStep.CREDIT);
        assertThat(state.executedSteps()).containsExactly(SagaStep.VALIDATION);
        assertThat(state.updatedAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void restore_rehydrates_persisted_state() {
        SagaExecutionState restored = SagaExecutionState.restore(
                SagaStep.DEBIT,
                java.util.Set.of(SagaStep.VALIDATION, SagaStep.RISK_CHECK, SagaStep.RESERVATION),
                null, null, NOW);

        assertThat(restored.sagaStep()).isEqualTo(SagaStep.DEBIT);
        assertThat(restored.executedSteps()).hasSize(3);
        assertThat(restored.updatedAt()).isEqualTo(NOW);
    }
}