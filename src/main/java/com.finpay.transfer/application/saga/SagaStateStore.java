package com.finpay.transfer.application.saga;

import com.finpay.transfer.domain.event.TransferCompletedEvent;
import com.finpay.transfer.domain.event.TransferFailedEvent;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists saga state changes atomically with their outbox events (ADR-0004).
 *
 * <p>The coordinator never runs a remote call inside a transaction (AGENTS.md
 * Rule 5): every step's external command happens <em>before</em> this store is
 * called, and each store call is its own transaction. A crash at any point
 * leaves the DB in a consistent, recoverable state.
 */
@Service
public class SagaStateStore {

    private final TransferRepository transferRepository;
    private final TransferCompletedEventPublisher completedEventPublisher;
    private final TransferFailedEventPublisher failedEventPublisher;

    public SagaStateStore(
            TransferRepository transferRepository,
            TransferCompletedEventPublisher completedEventPublisher,
            TransferFailedEventPublisher failedEventPublisher) {
        this.transferRepository = transferRepository;
        this.completedEventPublisher = completedEventPublisher;
        this.failedEventPublisher = failedEventPublisher;
    }

    /** Persists a forward step transition (executed steps + next saga step). */
    @Transactional
    public void recordStepExecuted(Transfer transfer) {
        transferRepository.save(transfer);
    }

    /** Persists a compensation step transition. */
    @Transactional
    public void recordStepCompensated(Transfer transfer) {
        transferRepository.save(transfer);
    }

    /** Persists the compensation-path entry (compensating + failure reason). */
    @Transactional
    public void markCompensating(Transfer transfer) {
        transferRepository.save(transfer);
    }

    /** Persists terminal success and publishes {@code TransferCompleted}. */
    @Transactional
    public void complete(Transfer transfer, Instant occurredAt) {
        transferRepository.save(transfer);
        completedEventPublisher.publish(TransferCompletedEvent.of(transfer, occurredAt));
    }

    /** Persists terminal failure and publishes {@code TransferFailed}. */
    @Transactional
    public void fail(Transfer transfer, Instant occurredAt) {
        transferRepository.save(transfer);
        failedEventPublisher.publish(TransferFailedEvent.of(transfer, occurredAt));
    }
}
