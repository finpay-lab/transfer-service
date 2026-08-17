package com.finpay.transfer.application.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finpay.transfer.application.saga.handler.CreditStepHandler;
import com.finpay.transfer.application.saga.handler.DebitStepHandler;
import com.finpay.transfer.application.saga.handler.FinalizationStepHandler;
import com.finpay.transfer.application.saga.handler.ReservationStepHandler;
import com.finpay.transfer.application.saga.handler.RiskCheckStepHandler;
import com.finpay.transfer.application.saga.handler.SagaStepHandler;
import com.finpay.transfer.application.saga.handler.ValidationStepHandler;
import com.finpay.transfer.application.saga.port.FundsReservationPort;
import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.RiskDecision;
import com.finpay.transfer.domain.event.TransferCompletedEvent;
import com.finpay.transfer.domain.event.TransferFailedEvent;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferSagaCoordinatorTest {

    private static final UUID FROM = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID TO = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");
    private static final UUID KEY = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID RESERVATION_ID = UUID.fromString("aaaaaaa1-1111-1111-1111-111111111111");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC);

    private final TransferRepository repository = mock(TransferRepository.class);
    private final TransferCompletedEventPublisher completedPublisher = mock(TransferCompletedEventPublisher.class);
    private final TransferFailedEventPublisher failedPublisher = mock(TransferFailedEventPublisher.class);
    private final FundsReservationPort reservationPort = mock(FundsReservationPort.class);
    private final LedgerPostingPort ledgerPort = mock(LedgerPostingPort.class);
    private final RiskCheckPort riskPort = mock(RiskCheckPort.class);

    private TransferSagaCoordinator coordinator;

    @BeforeEach
    void setUp() {
        List<SagaStepHandler> handlers = List.of(
                new ValidationStepHandler(),
                new ReservationStepHandler(reservationPort),
                new RiskCheckStepHandler(riskPort),
                new DebitStepHandler(ledgerPort),
                new CreditStepHandler(ledgerPort),
                new FinalizationStepHandler());
        when(reservationPort.reserve(any(), any(), any(), any())).thenReturn(RESERVATION_ID);
        when(riskPort.evaluate(any(), any(), any())).thenReturn(RiskDecision.APPROVED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        coordinator = new TransferSagaCoordinator(
                repository, new SagaStateStore(repository, completedPublisher, failedPublisher), handlers, CLOCK);
    }

    private Transfer newTransfer() {
        return Transfer.create(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY.toString(),
                Instant.parse("2026-08-17T09:00:00Z"));
    }

    private void stubFind(Transfer transfer) {
        when(repository.findById(transfer.transferId())).thenReturn(Optional.of(transfer));
    }

    @Test
    void happyPath_drivesAllStepsAndCompletes() {
        Transfer transfer = newTransfer();
        stubFind(transfer);

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.FINALIZATION);
        assertThat(transfer.execution().executedSteps()).containsExactlyInAnyOrder(
                SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK,
                SagaStep.DEBIT, SagaStep.CREDIT, SagaStep.FINALIZATION);
        verify(reservationPort).reserve(transfer.transferId(), FROM, new BigDecimal("150.00"), "EUR");
        verify(ledgerPort).debit(transfer.transferId(), FROM, new BigDecimal("150.00"), "EUR");
        verify(ledgerPort).credit(transfer.transferId(), TO, new BigDecimal("150.00"), "EUR");
        verify(completedPublisher).publish(any(TransferCompletedEvent.class));
        verify(failedPublisher, never()).publish(any());
    }

    @Test
    void failureAtCredit_compensatesInReverseOrderAndFails() {
        Transfer transfer = newTransfer();
        stubFind(transfer);
        when(ledgerPort.credit(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Destination account is closed"));

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.COMPENSATION);
        assertThat(transfer.execution().failedAtStep()).isEqualTo(SagaStep.CREDIT);
        assertThat(transfer.execution().failureReason()).contains("closed");
        assertThat(transfer.execution().compensatedSteps()).containsExactlyInAnyOrder(
                SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK, SagaStep.DEBIT);

        // Compensation reverses executed steps in reverse order.
        verify(ledgerPort).reverseDebit(transfer.transferId(), FROM, new BigDecimal("150.00"), "EUR");
        verify(reservationPort).release(RESERVATION_ID);
        verify(failedPublisher).publish(any(TransferFailedEvent.class));
        verify(completedPublisher, never()).publish(any());
    }

    @Test
    void resumeAfterCrash_doesNotReExecuteCompletedSteps() {
        // Simulates a crash right after RISK_CHECK was executed and persisted
        // (executed steps + next step DEBIT are the durable saga state).
        Transfer transfer = Transfer.restore(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY.toString(),
                Instant.parse("2026-08-17T09:00:00Z"),
                TransferStatus.CREATED, SagaStep.DEBIT,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK),
                Set.of(), false, RESERVATION_ID, null, null,
                Instant.parse("2026-08-17T09:01:00Z"));
        stubFind(transfer);

        coordinator.run(transfer.transferId());

        verify(reservationPort, never()).reserve(any(), any(), any(), any());
        verify(riskPort, never()).evaluate(any(), any(), any());
        verify(ledgerPort).debit(transfer.transferId(), FROM, new BigDecimal("150.00"), "EUR");
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void resumeMidCompensation_finishesRemainingCompensation() {
        // Crash after DEBIT was compensated but before RESERVATION was.
        Transfer transfer = Transfer.restore(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY.toString(),
                Instant.parse("2026-08-17T09:00:00Z"),
                TransferStatus.CREATED, SagaStep.COMPENSATION,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK, SagaStep.DEBIT),
                Set.of(SagaStep.DEBIT), true, RESERVATION_ID, "Destination account is closed",
                SagaStep.CREDIT, Instant.parse("2026-08-17T09:02:00Z"));
        stubFind(transfer);

        coordinator.run(transfer.transferId());

        // DEBIT was already compensated before the crash; only RESERVATION (and
        // the no-op steps) must be compensated now.
        verify(ledgerPort, never()).reverseDebit(any(), any(), any(), any());
        verify(reservationPort).release(RESERVATION_ID);
        assertThat(transfer.execution().compensatedSteps()).containsExactlyInAnyOrder(
                SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK, SagaStep.DEBIT);
        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void terminalTransfer_isLeftUntouched() {
        Transfer transfer = newTransfer();
        transfer.complete(CLOCK.instant());
        stubFind(transfer);

        coordinator.run(transfer.transferId());

        verify(reservationPort, never()).reserve(any(), any(), any(), any());
        verify(repository, never()).save(any());
    }
}
