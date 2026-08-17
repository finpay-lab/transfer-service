package com.finpay.transfer.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.transfer.application.saga.TransferCompletedEventPublisher;
import com.finpay.transfer.domain.event.TransferCompletedEvent;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes {@code TransferCompleted} events to the outbox table. Runs inside the
 * terminal-state transaction (REQUIRED joins it), so the event is stored
 * atomically with the aggregate (ADR-0004); a failed publish can be retried
 * without a second event because consumers dedupe by {@code eventId}.
 */
@Component
public class OutboxTransferCompletedEventPublisher implements TransferCompletedEventPublisher {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxTransferCompletedEventPublisher(
            OutboxJpaRepository outboxJpaRepository,
            ObjectMapper objectMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(TransferCompletedEvent event) {
        outboxJpaRepository.save(toEntity(event));
    }

    private OutboxJpaEntity toEntity(TransferCompletedEvent event) {
        try {
            return new OutboxJpaEntity(
                    UUID.randomUUID(),
                    event.eventId(),
                    event.eventType(),
                    event.payload().transferId(),
                    event.partitionKey(),
                    objectMapper.writeValueAsString(event),
                    event.occurredAt(),
                    null);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize TransferCompletedEvent", e);
        }
    }
}
