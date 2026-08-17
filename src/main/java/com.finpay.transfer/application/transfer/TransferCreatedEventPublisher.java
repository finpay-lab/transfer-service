package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.event.TransferCreatedEvent;

/**
 * Port for emitting a {@code TransferCreated} event. Implementations write the
 * event to the outbox within the same database transaction as the aggregate
 * change (ADR-0004), keeping DB state and event emission atomic.
 */
public interface TransferCreatedEventPublisher {

    void publish(TransferCreatedEvent event);
}