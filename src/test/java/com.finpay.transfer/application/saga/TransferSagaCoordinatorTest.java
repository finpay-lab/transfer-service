package com.finpay.transfer.application.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.finpay.transfer.application.saga.handler.CreditStepHandler;
import com.finpay.transfer.application.saga.handler.DebitStepHandler;
import com.finpay.transfer.application.saga.handler.FinalizationStepHandler;
import com.finpay.transfer.application.saga.handler.ReservationStepHandler;
import com.finpay.transfer.application.saga.handler.RiskCheckStepHandler;
import com.finpay.transfer.application.saga.handler.ValidationStepHandler;
import com.finpay.transfer.application.transfer.FakeTransferRepository;
import com.finpay.transfer.domain.event.TransferCompletedEvent;
import com.finpay.transfer.domain.event.TransferFailedEvent;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferStatus;
import com.finpay.transfer.infrastructure.saga.InMemoryFundsReservationService;
import com.finpay.transfer.infrastructure.saga.InMemoryLedgerService;
import com.finpay.transfer.infrastructure.saga.InMemoryRiskCheckService;
import com.finpay.transfer.infrastructure.saga.InMemoryTransferValidationService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Drives the full saga through its persisted step state using the real
 * handlers and in-memory port stand-ins (no Spring, no DB).
 */
@ExtendWith(MockitoExtension.class)
class TransferSagaCoordinatorTest {

    private static final UUID CUSTOMER = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID FROM = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");
    private static final UUID TO = UUID.fromString("22334455-6677-8899-aabb-ccddeeff0011");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-12T06:30:00Z"), ZoneOffset.UTC);

    private FakeTransferRepository repository;
    private InMemoryTransferValidationService validationService;
    private InMemoryRiskCheckService riskCheckService;
    private InMemoryFundsReservationService reservationService;
    private InMemoryLedgerService ledgerService;
    private TransferSagaCoordinator coordinator;

    @Mock
    private TransferCompletedEventPublisher completedEventPublisher;

    @Mock
    private TransferFailedEventPublisher failedEventPublisher;

    @BeforeEach
    void setUp() {
        repository = new FakeTransferRepository();
        validationService = new InMemoryTransferValidationService();
        validationService.registerCustomer(CUSTOMER);
        validationService.registerAccount(FROM);
        validationService.registerAccount(TO);
        riskCheckService = new InMemoryRiskCheckService();
        reservationService = new InMemoryFundsReservationService();
        ledgerService = new InMemoryLedgerService();

        SagaStateStore stateStore = new SagaStateStore(repository, completedEventPublisher, failedEventPublisher);
        coordinator = new TransferSagaCoordinator(
                repository,
                stateStore,
                List.of(
                        new ValidationStepHandler(validationService),
                        new RiskCheckStepHandler(riskCheckService),
                        new ReservationStepHandler(reservationService),
                        new DebitStepHandler(ledgerService),
                        new CreditStepHandler(ledgerService),
                        new FinalizationStepHandler()),
                CLOCK);
    }

    @AfterEach
    void tearDown() {
        validationService.clearState();
        riskCheckService.clearState();
        reservationService.clearState();
        ledgerService.clearState();
    }

    private Transfer newTransfer() {
        return Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("150.00"), "EUR", UUID.randomUUID().toString(), CLOCK.instant());
    }

    @Test
    void happy_path_runs_all_six_steps_and_completes() {
        Transfer transfer = newTransfer();
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        assertThat(repository.findById(transfer.transferId()).orElseThrow().status())
                .isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.execution().executedSteps())
                .containsExactlyInAnyOrder(
                        SagaStep.VALIDATION, SagaStep.RISK_CHECK, SagaStep.RESERVATION,
                        SagaStep.DEBIT, SagaStep.CREDIT, SagaStep.FINALIZATION);
        assertThat(reservationService.get(transfer.transferId())).isNotNull();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "CREDIT")).isTrue();
        verify(completedEventPublisher).publish(org.mockito.ArgumentMatchers.any(TransferCompletedEvent.class));
    }

    @Test
    void run_is_idempotent_when_transfer_is_already_terminal() {
        Transfer transfer = newTransfer();
        transfer.complete(CLOCK.instant());
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        verify(completedEventPublisher, org.mockito.Mockito.never())
                .publish(org.mockito.ArgumentMatchers.any(TransferCompletedEvent.class));
    }

    @Test
    void run_resumes_from_persisted_state_after_a_crash() {
        Transfer transfer = newTransfer();
        // Simulate a crash after VALIDATION + RISK_CHECK + RESERVATION executed.
        transfer.execution().recordExecuted(SagaStep.VALIDATION, CLOCK.instant());
        transfer.execution().recordExecuted(SagaStep.RISK_CHECK, CLOCK.instant());
        transfer.execution().recordExecuted(SagaStep.RESERVATION, CLOCK.instant());
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(ledgerService.hasPosting(transfer.transferId(), "DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "CREDIT")).isTrue();
    }

    @Test
    void validation_failure_fails_the_saga_at_validation_step() {
        validationService.clearState(); // customer unknown -> validation fails
        Transfer transfer = newTransfer();
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.execution().failedAtStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(transfer.execution().failureReason()).isNotNull();
        assertThat(reservationService.get(transfer.transferId())).isNull();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "DEBIT")).isFalse();
        verify(failedEventPublisher).publish(org.mockito.ArgumentMatchers.any(TransferFailedEvent.class));
    }

    @Test
    void risk_rejection_fails_before_any_money_moves() {
        riskCheckService.rejectCustomer(CUSTOMER);
        Transfer transfer = newTransfer();
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.execution().failedAtStep()).isEqualTo(SagaStep.RISK_CHECK);
        assertThat(reservationService.get(transfer.transferId())).isNull();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "DEBIT")).isFalse();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "CREDIT")).isFalse();
    }

    @Test
    void credit_failure_fails_the_saga_and_records_debit_executed() {
        ledgerService.markAccountClosed(TO);
        Transfer transfer = newTransfer();
        repository.save(transfer);

        coordinator.run(transfer.transferId());

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.execution().failedAtStep()).isEqualTo(SagaStep.CREDIT);
        assertThat(transfer.execution().executedSteps())
                .contains(SagaStep.VALIDATION, SagaStep.RISK_CHECK, SagaStep.RESERVATION, SagaStep.DEBIT)
                .doesNotContain(SagaStep.CREDIT, SagaStep.FINALIZATION);
        assertThat(ledgerService.hasPosting(transfer.transferId(), "DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transfer.transferId(), "CREDIT")).isFalse();
        verify(failedEventPublisher).publish(org.mockito.ArgumentMatchers.any(TransferFailedEvent.class));
    }
}