package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.event.TransferCreatedEvent;
import com.finpay.transfer.domain.transfer.DuplicateIdempotencyKeyException;
import com.finpay.transfer.domain.transfer.IdempotencyConflictException;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent transfer creation (AGENTS.md Rule 6).
 *
 * <p>Contract:
 * <ul>
 *   <li>New idempotency key &rarr; persist transfer in status
 *       CREATED/VALIDATION and write the {@code TransferCreated} event to the
 *       outbox, all in one transaction.</li>
 *   <li>Same key, same payload &rarr; replay the existing transfer (no
 *       duplicate, no second event).</li>
 *   <li>Same key, different payload &rarr; {@link IdempotencyConflictException}.</li>
 *   <li>Concurrent duplicates are reconciled by the DB unique constraint on
 *       {@code idempotency_key}; the loser surfaces
 *       {@link DuplicateIdempotencyKeyException} and must retry with the same
 *       key, which then replays the winner's transfer.</li>
 * </ul>
 *
 * <p>No remote calls happen inside the transaction (AGENTS.md Rule 5): the
 * outbox write is local DB work; the actual Kafka publish is a separate relay.
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
        Transfer existing = transferRepository
                .findByIdempotencyKey(command.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            if (existing.matches(
                    command.customerId(),
                    command.sourceAccountId(),
                    command.destinationAccountId(),
                    command.amount(),
                    command.currency())) {
                return CreateTransferResult.replayed(existing);
            }
            throw new IdempotencyConflictException(command.idempotencyKey());
        }

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

        return CreateTransferResult.created(transfer);
    }
}