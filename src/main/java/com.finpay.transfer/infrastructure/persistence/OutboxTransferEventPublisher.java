package com.finpay.transfer.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.transfer.application.saga.TransferCompletedEventPublisher;
import com.finpay.transfer.application.saga.TransferFailedEventPublisher;
import com.finpay.transfer.application.transfer.TransferCreatedEventPublisher;
import com.finpay.transfer.domain.event.TransferCompletedEvent;
import com.finpay.transfer.domain.event.TransferCreatedEvent;
import com.finpay.transfer.domain.event.TransferFailedEvent;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Outbox-backed publisher for all transfer events (ADR-0004).
 *
 * <p>Each event is serialized to an outbox row inside the caller's
 * transaction — the business change and the event envelope commit together, so
 * a crash before publish never loses an event. A relay (later phase) publishes
 * unpublished rows to Kafka. Callers must run inside a transaction.
 */
@Component
public class OutboxTransferEventPublisher
        implements TransferCreatedEventPublisher, TransferCompletedEventPublisher, TransferFailedEventPublisher {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxTransferEventPublisher(
            OutboxJpaRepository outboxJpaRepository,
            ObjectMapper objectMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(TransferCreatedEvent event) {
        enqueue(event.eventType(), event.partitionKey(), event);
    }

    @Override
    public void publish(TransferCompletedEvent event) {
        enqueue(event.eventType(), event.partitionKey(), event);
    }

    @Override
    public void publish(TransferFailedEvent event) {
        enqueue(event.eventType(), event.partitionKey(), event);
    }

    private void enqueue(String eventType, String partitionKey, Object event) {
        UUID aggregateId = UUID.fromString(partitionKey);
        outboxJpaRepository.save(new OutboxJpaEntity(
                UUID.randomUUID(),
                eventIdOf(event),
                eventType,
                aggregateId,
                partitionKey,
                writeJson(event),
                Instant.now()));
    }

    private UUID eventIdOf(Object event) {
        return switch (event) {
            case TransferCreatedEvent e -> e.eventId();
            case TransferCompletedEvent e -> e.eventId();
            case TransferFailedEvent e -> e.eventId();
            default -> throw new IllegalStateException("Unsupported event " + event.getClass());
        };
    }

    private String writeJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}