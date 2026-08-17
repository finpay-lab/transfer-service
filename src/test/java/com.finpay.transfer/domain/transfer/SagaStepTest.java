package com.finpay.transfer.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SagaStepTest {

    @Test
    void forward_flow_follows_validate_then_risk_then_reserve_then_ledger_then_finalize() {
        assertThat(SagaStep.VALIDATION.next()).contains(SagaStep.RISK_CHECK);
        assertThat(SagaStep.RISK_CHECK.next()).contains(SagaStep.RESERVATION);
        assertThat(SagaStep.RESERVATION.next()).contains(SagaStep.DEBIT);
        assertThat(SagaStep.DEBIT.next()).contains(SagaStep.CREDIT);
        assertThat(SagaStep.CREDIT.next()).contains(SagaStep.FINALIZATION);
        assertThat(SagaStep.FINALIZATION.next()).isEmpty();
    }

    @Test
    void notification_and_compensation_are_not_forward_steps() {
        assertThat(SagaStep.NOTIFICATION.next()).isEmpty();
        assertThat(SagaStep.COMPENSATION.next()).isEmpty();
    }

    @Test
    void order_reflects_execution_sequence() {
        assertThat(SagaStep.VALIDATION.order()).isLessThan(SagaStep.RISK_CHECK.order());
        assertThat(SagaStep.RISK_CHECK.order()).isLessThan(SagaStep.RESERVATION.order());
        assertThat(SagaStep.RESERVATION.order()).isLessThan(SagaStep.DEBIT.order());
        assertThat(SagaStep.DEBIT.order()).isLessThan(SagaStep.CREDIT.order());
        assertThat(SagaStep.CREDIT.order()).isLessThan(SagaStep.FINALIZATION.order());
    }
}