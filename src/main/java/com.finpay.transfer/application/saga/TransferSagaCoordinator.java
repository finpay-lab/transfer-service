package com.finpay.transfer.application.saga;

import com.finpay.transfer.application.saga.handler.SagaStepHandler;
import com.finpay.transfer.application.transfer.TransferNotFoundException;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.SagaStepExecutionException;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrator of the transfer SAGA (ADR-0003).
 *
 * <p>Drives the persisted state of a transfer deterministically. Every forward
 * step is executed through a {@link SagaStepHandler} (external commands) and
 * its result persisted via {@link SagaStateStore} in its own transaction
 * (AGENTS.md Rule 5: no remote calls inside {@code @Transactional}).
 *
 * <p>On a step failure the saga is marked terminal-{@code FAILED} and the
 * failure reason / step are persisted (and surfaced on the
 * {@code TransferFailed} event). Because all state is persisted, calling
 * {@link #run} again after a crash resumes exactly where the saga stopped —
 * this is what the recovery job relies on.
 */
@Service
public class TransferSagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaCoordinator.class);

    private final TransferRepository transferRepository;
    private final SagaStateStore stateStore;
    private final Map<SagaStep, SagaStepHandler> handlers;
    private final Clock clock;

    public TransferSagaCoordinator(
            TransferRepository transferRepository,
            SagaStateStore stateStore,
            List<SagaStepHandler> handlers,
            Clock clock) {
        this.transferRepository = transferRepository;
        this.stateStore = stateStore;
        this.handlers = new EnumMap<>(SagaStep.class);
        for (SagaStepHandler handler : handlers) {
            this.handlers.put(handler.step(), handler);
        }
        this.clock = clock;
    }

    /**
     * Drives the saga forward until it reaches a terminal state. Safe to call
     * repeatedly: it re-drives whatever the persisted state requires.
     */
    public void run(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
        if (transfer.isTerminal()) {
            return;
        }
        while (!transfer.isTerminal()) {
            if (!advanceOneStep(transfer)) {
                return;
            }
        }
    }

    /** Executes the current forward step and persists the transition. */
    private boolean advanceOneStep(Transfer transfer) {
        SagaStep step = transfer.sagaStep();
        SagaStepHandler handler = handlerFor(step);
        Instant now = clock.instant();
        try {
            handler.execute(transfer);
        } catch (SagaStepExecutionException e) {
            log.warn("saga step {} failed for transfer {}: {}", step, transfer.transferId(), e.getMessage());
            transfer.execution().markFailed(e.getMessage(), step, now);
            transfer.fail(now);
            stateStore.fail(transfer, now);
            return false;
        }
        transfer.execution().recordExecuted(step, now);
        if (step == SagaStep.FINALIZATION) {
            transfer.complete(now);
            stateStore.complete(transfer, now);
            return false;
        }
        stateStore.recordStepExecuted(transfer);
        return true;
    }

    private SagaStepHandler handlerFor(SagaStep step) {
        SagaStepHandler handler = handlers.get(step);
        if (handler == null) {
            throw new IllegalStateException("No saga step handler registered for " + step);
        }
        return handler;
    }
}
