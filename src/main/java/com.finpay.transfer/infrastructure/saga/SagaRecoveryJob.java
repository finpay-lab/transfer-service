package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.TransferSagaCoordinator;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Crash-recovery driver (ADR-0003). On startup and periodically it scans for
 * sagas that are still non-terminal (status CREATED) and re-drives them via
 * {@link TransferSagaCoordinator}. Because every step transition is persisted
 * and every step/compensation is idempotent, "resume on restart" is just
 * re-running the coordinator on the persisted state.
 *
 * <p>In this lab the recovery job also acts as the saga runner: transfers are
 * created via the API (writing {@code TransferCreated} to the outbox), and the
 * next recovery tick drives the money-flow steps to completion. In production
 * the step commands would be dispatched through Kafka and only <em>stuck</em>
 * sagas (e.g. no progress for N seconds) would be re-driven.
 *
 * <p>Single-instance assumption: concurrent recovery runs are guarded by the
 * {@code transfer.saga.recovery.interval-ms} throttle. Multi-instance
 * deployments need a lease/claim to avoid two nodes driving the same saga.
 *
 * <p>Can be switched off for tests ({@code transfer.saga.recovery.enabled}).
 */
@Component
@ConditionalOnProperty(
        name = "transfer.saga.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SagaRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryJob.class);

    private static final int RECOVERY_LIMIT = 100;

    private final TransferRepository transferRepository;
    private final TransferSagaCoordinator coordinator;

    public SagaRecoveryJob(
            TransferRepository transferRepository,
            TransferSagaCoordinator coordinator) {
        this.transferRepository = transferRepository;
        this.coordinator = coordinator;
    }

    @Scheduled(
            initialDelayString = "${transfer.saga.recovery.initial-delay-ms:1000}",
            fixedDelayString = "${transfer.saga.recovery.interval-ms:5000}")
    public void recoverStuckTransfers() {
        List<Transfer> stuck = transferRepository.findNonTerminal(RECOVERY_LIMIT);
        if (stuck.isEmpty()) {
            return;
        }
        log.info("saga recovery: driving {} non-terminal transfer(s)", stuck.size());
        for (Transfer transfer : stuck) {
            UUID transferId = transfer.transferId();
            try {
                coordinator.run(transferId);
            } catch (Exception e) {
                log.error("saga recovery failed for transfer {}", transferId, e);
            }
        }
    }
}