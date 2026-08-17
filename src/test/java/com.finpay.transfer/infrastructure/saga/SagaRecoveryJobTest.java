package com.finpay.transfer.infrastructure.saga;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finpay.transfer.application.saga.TransferSagaCoordinator;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SagaRecoveryJobTest {

    private final TransferRepository repository = mock(TransferRepository.class);
    private final TransferSagaCoordinator coordinator = mock(TransferSagaCoordinator.class);
    private final SagaRecoveryJob job = new SagaRecoveryJob(repository, coordinator);

    private Transfer stuckTransfer() {
        return Transfer.create(
                UUID.randomUUID(),
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"),
                new BigDecimal("150.00"), "EUR", UUID.randomUUID().toString(),
                Instant.parse("2026-08-17T09:00:00Z"));
    }

    @Test
    void recoveryJob_drivesEveryNonTerminalTransfer() {
        Transfer first = stuckTransfer();
        Transfer second = stuckTransfer();
        when(repository.findNonTerminal(100)).thenReturn(List.of(first, second));

        job.recoverStuckTransfers();

        verify(coordinator).run(first.transferId());
        verify(coordinator).run(second.transferId());
    }

    @Test
    void recoveryJob_noCandidates_doesNothing() {
        when(repository.findNonTerminal(100)).thenReturn(List.of());

        job.recoverStuckTransfers();

        verify(coordinator, never()).run(any(UUID.class));
    }

    @Test
    void recoveryJob_isolatesCoordinatorFailure() {
        Transfer first = stuckTransfer();
        when(repository.findNonTerminal(100)).thenReturn(List.of(first));
        when(coordinator.run(first.transferId())).thenThrow(new IllegalStateException("boom"));

        job.recoverStuckTransfers();

        verify(coordinator, times(1)).run(first.transferId());
    }
}
