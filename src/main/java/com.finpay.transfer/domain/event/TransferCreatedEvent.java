package com.finpay.transfer.domain.event;

import com.finpay.transfer.domain.transfer.Transfer;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a transfer saga is initiated. Shape matches the
 * v1 contract contracts/events/v1/TransferCreated.json. The event is written
 * to the outbox atomically with the aggregate (ADR-0004) and later emitted to
 * the {@code finpay.transfer} topic by the outbox relay.
 */
public record TransferCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        String partitionKey,
        Payload payload) {

    public static final String EVENT_TYPE = "TransferCreated";
    public static final int VERSION = 1;

    public static TransferCreatedEvent of(Transfer transfer, Instant occurredAt) {
        return new TransferCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                occurredAt,
                VERSION,
                transfer.transferId().toString(),
                new Payload(
                        transfer.transferId(),
                        transfer.sourceAccountId(),
                        transfer.destinationAccountId(),
                        transfer.amount().toPlainString(),
                        transfer.currency(),
                        transfer.status().name(),
                        transfer.sagaStep().name()));
    }

    public record Payload(
            UUID transferId,
            UUID from,
            UUID to,
            String amount,
            String currency,
            String status,
            String sagaStep) {}
}
