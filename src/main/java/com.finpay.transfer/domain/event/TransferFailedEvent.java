package com.finpay.transfer.domain.event;

import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a transfer saga fails. Shape matches the v1
 * contract contracts/events/v1/TransferFailed.json. Written to the outbox
 * atomically with the terminal aggregate state (ADR-0004) and emitted to the
 * {@code finpay.transfer} topic by the outbox relay.
 *
 * <p>{@code sagaStep} carries the step at which the saga failed (per the
 * contract), while {@code failureReason} holds the human-readable cause.
 */
public record TransferFailedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        String partitionKey,
        Payload payload) {

    public static final String EVENT_TYPE = "TransferFailed";
    public static final int VERSION = 1;

    public static TransferFailedEvent of(Transfer transfer, Instant occurredAt) {
        return new TransferFailedEvent(
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
                        TransferStatus.FAILED.name(),
                        transfer.execution().failedAtStep().name(),
                        transfer.execution().failureReason()));
    }

    public record Payload(
            UUID transferId,
            UUID from,
            UUID to,
            String amount,
            String currency,
            String status,
            String sagaStep,
            String failureReason) {}
}
