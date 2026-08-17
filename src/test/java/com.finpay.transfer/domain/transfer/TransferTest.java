package com.finpay.transfer.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransferTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID FROM = UUID.randomUUID();
    private static final UUID TO = UUID.randomUUID();

    private Transfer newTransfer() {
        return Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("150.00"), "EUR", UUID.randomUUID().toString(),
                Instant.parse("2026-08-12T06:30:00Z"));
    }

    @Test
    void create_starts_in_created_status_and_validation_step() {
        Transfer transfer = newTransfer();

        assertThat(transfer.status()).isEqualTo(TransferStatus.CREATED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(transfer.execution().executedSteps()).isEmpty();
        assertThat(transfer.isTerminal()).isFalse();
    }

    @Test
    void complete_from_created_is_legal() {
        Transfer transfer = newTransfer();

        transfer.complete(Instant.now());

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.isTerminal()).isTrue();
    }

    @Test
    void fail_from_created_is_legal() {
        Transfer transfer = newTransfer();

        transfer.fail(Instant.now());

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.isTerminal()).isTrue();
    }

    @Test
    void transition_from_terminal_status_is_rejected() {
        Transfer transfer = newTransfer();
        transfer.complete(Instant.now());

        assertThatThrownBy(() -> transfer.fail(Instant.now()))
                .isInstanceOf(IllegalTransferStateTransitionException.class);

        assertThatThrownBy(() -> transfer.transitionTo(TransferStatus.CREATED))
                .isInstanceOf(IllegalTransferStateTransitionException.class);
    }

    @Test
    void restore_rehydrates_status_and_saga_state() {
        Transfer original = newTransfer();
        original.execution().recordExecuted(SagaStep.VALIDATION, Instant.now());
        original.execution().recordExecuted(SagaStep.RISK_CHECK, Instant.now());
        original.complete(Instant.now());

        Transfer restored = Transfer.restore(
                original.transferId(), original.customerId(),
                original.sourceAccountId(), original.destinationAccountId(),
                original.amount(), original.currency(), original.idempotencyKey(),
                original.createdAt(), original.status(), original.sagaStep(),
                original.execution().executedSteps(),
                original.execution().failureReason(),
                original.execution().failedAtStep(),
                original.execution().updatedAt());

        assertThat(restored.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(restored.execution().executedSteps())
                .containsExactlyInAnyOrder(SagaStep.VALIDATION, SagaStep.RISK_CHECK);
        assertThat(restored.isTerminal()).isTrue();
    }

    @Test
    void create_rejects_invalid_input() {
        assertThatThrownBy(() -> Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("-5.00"), "EUR", UUID.randomUUID().toString(),
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("5.00"), "eur", UUID.randomUUID().toString(),
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transfer.create(
                UUID.randomUUID(), null, FROM, TO,
                new BigDecimal("5.00"), "EUR", UUID.randomUUID().toString(),
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}