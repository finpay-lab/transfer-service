package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.event.TransferCreatedEvent;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a transfer aggregate and persists it atomically with the
 * {@code TransferCreated} outbox event (ADR-0004).
 *
 * <p>No remote calls happen here (AGENTS.md Rule 5): the saga is driven
 * asynchronously afterwards by the orchestrator, so this transaction commits
 * fast. The unique constraint on {@code idempotency_key} (AGENTS.md Rule 6)
 * guards at-most-one transfer per key at the schema level.
 */
@Service
public class CreateTransferUseCase {

    private final TransferRepository transferRepository;
    private final TransferCreatedEventPublisher eventPublisher;
    private final Clock clock;

    public CreateTransferUseCase(
            TransferRepository transferRepository,
            TransferCreatedEventPublisher eventPublisher,
            Clock clock) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public CreateTransferResult execute(CreateTransferCommand command) {
        Transfer transfer = Transfer.create(
                UUID.randomUUID(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.currency(),
                command.idempotencyKey(),
                clock.instant());
        transferRepository.save(transfer);
        eventPublisher.publish(TransferCreatedEvent.of(transfer, clock.instant()));
        return CreateTransferResult.of(transfer);
    }
}
