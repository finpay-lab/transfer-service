package com.finpay.transfer.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.transfer.application.saga.TransferFailedEventPublisher;
import com.finpay.transfer.domain.event.TransferFailedEvent;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes {@code TransferFailed} events to the outbox table. Runs inside the
 * terminal-state transaction (REQUIRED joins it), so the event is stored
 * atomically with the aggregate (ADR-0004); a failed publish can be retried
 * without a second event because consumers dedupe by {@code eventId}.
 */
@Component
public class OutboxTransferFailedEventPublisher implements TransferFailedEventPublisher {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxTransferFailedEventPublisher(
            OutboxJpaRepository outboxJpaRepository,
            ObjectMapper objectMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(TransferFailedEvent event) {
        outboxJpaRepository.save(toEntity(event));
    }

    private OutboxJpaEntity toEntity(TransferFailedEvent event) {
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
            throw new IllegalStateException("Failed to serialize TransferFailedEvent", e);
        }
    }
}