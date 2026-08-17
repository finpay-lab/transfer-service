package com.finpay.transfer.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransferTest {

    private static final UUID FROM = UUID.randomUUID();
    private static final UUID TO = UUID.randomUUID();
    private static final String KEY = UUID.randomUUID().toString();

    private Transfer newTransfer() {
        return Transfer.create(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY, Instant.parse("2026-08-17T10:00:00Z"));
    }

    @Test
    void create_setsInitialSagaState() {
        Transfer transfer = newTransfer();
        assertThat(transfer.status()).isEqualTo(TransferStatus.CREATED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(transfer.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> Transfer.create(UUID.randomUUID(), FROM, TO, BigDecimal.ZERO, "EUR", KEY, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void create_rejectsInvalidCurrency() {
        assertThatThrownBy(() -> Transfer.create(UUID.randomUUID(), FROM, TO, new BigDecimal("1.00"), "eu", KEY, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void create_rejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> Transfer.create(UUID.randomUUID(), FROM, TO, new BigDecimal("1.00"), "EUR", " ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
    }

    @Test
    void legalTransition_fromCreatedToCompletedIsApplied() {
        Transfer transfer = newTransfer();
        transfer.transitionTo(TransferStatus.COMPLETED);
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void legalTransition_fromCreatedToFailedIsApplied() {
        Transfer transfer = newTransfer();
        transfer.transitionTo(TransferStatus.FAILED);
        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void illegalTransition_isRejected() {
        Transfer transfer = newTransfer();
        transfer.transitionTo(TransferStatus.COMPLETED);
        assertThatThrownBy(() -> transfer.transitionTo(TransferStatus.FAILED))
                .isInstanceOf(IllegalTransferStateTransitionException.class);
    }

    @Test
    void transitionFromTerminalState_isRejected() {
        Transfer transfer = newTransfer();
        transfer.transitionTo(TransferStatus.FAILED);
        assertThatThrownBy(() -> transfer.transitionTo(TransferStatus.COMPLETED))
                .isInstanceOf(IllegalTransferStateTransitionException.class);
    }

    @Test
    void reversalAfterCompletion_isLegal() {
        Transfer transfer = newTransfer();
        transfer.transitionTo(TransferStatus.COMPLETED);
        transfer.transitionTo(TransferStatus.REVERSED);
        assertThat(transfer.status()).isEqualTo(TransferStatus.REVERSED);
    }

    @Test
    void matches_comparesRequestDetails() {
        Transfer transfer = newTransfer();
        assertThat(transfer.matches(FROM, TO, new BigDecimal("150"), "EUR")).isTrue();
        assertThat(transfer.matches(FROM, TO, new BigDecimal("151.00"), "EUR")).isFalse();
        assertThat(transfer.matches(FROM, TO, new BigDecimal("150.00"), "USD")).isFalse();
        assertThat(transfer.matches(UUID.randomUUID(), TO, new BigDecimal("150.00"), "EUR")).isFalse();
    }

    @Test
    void restore_preservesStatusAndSagaStep() {
        Transfer transfer = Transfer.restore(
                UUID.randomUUID(), FROM, TO, new BigDecimal("10.00"), "EUR", KEY,
                Instant.parse("2026-08-17T10:00:00Z"), TransferStatus.COMPLETED, SagaStep.FINALIZATION);
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.FINALIZATION);
    }
}
