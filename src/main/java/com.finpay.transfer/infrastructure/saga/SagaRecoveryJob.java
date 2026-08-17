package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.TransferSagaCoordinator;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic driver of the transfer saga (ADR-0003 deterministic recovery).
 *
 * <p>Every non-terminal transfer is re-driven from its persisted saga state:
 * completed steps are skipped (handlers/ports are idempotent by transferId),
 * so a crash at any point is recovered by simply running the saga again.
 * This is what makes recovery state-in-DB, not state-in-memory.
 */
@Component
public class SagaRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryJob.class);

    private static final int BATCH_SIZE = 100;

    private final TransferRepository transferRepository;
    private final TransferSagaCoordinator coordinator;

    public SagaRecoveryJob(TransferRepository transferRepository, TransferSagaCoordinator coordinator) {
        this.transferRepository = transferRepository;
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${transfer.saga.recovery.interval-ms:5000}")
    public void recoverPendingSagas() {
        List<Transfer> pending = transferRepository.findNonTerminal(BATCH_SIZE);
        for (Transfer transfer : pending) {
            try {
                coordinator.run(transfer.transferId());
            } catch (RuntimeException e) {
                log.warn("saga recovery failed for transfer {}: {}", transfer.transferId(), e.getMessage());
            }
        }
    }
}