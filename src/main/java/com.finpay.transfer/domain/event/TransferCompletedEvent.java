package com.finpay.transfer.domain.event;

import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a transfer saga succeeds. Shape matches the v1
 * contract contracts/events/v1/TransferCompleted.json. Written to the outbox
 * atomically with the terminal aggregate state (ADR-0004) and emitted to the
 * {@code finpay.transfer} topic by the outbox relay.
 */
public record TransferCompletedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        String partitionKey,
        Payload payload) {

    public static final String EVENT_TYPE = "TransferCompleted";
    public static final int VERSION = 1;

    public static TransferCompletedEvent of(Transfer transfer, Instant occurredAt) {
        return new TransferCompletedEvent(
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
                        TransferStatus.COMPLETED.name(),
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
