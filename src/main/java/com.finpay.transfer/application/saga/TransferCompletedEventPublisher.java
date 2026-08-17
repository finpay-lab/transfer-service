package com.finpay.transfer.application.saga;

import com.finpay.transfer.domain.event.TransferCompletedEvent;

/**
 * Port for emitting a {@code TransferCompleted} event. Implementations write
 * the event to the outbox within the same database transaction as the terminal
 * aggregate change (ADR-0004), keeping DB state and event emission atomic.
 */
public interface TransferCompletedEventPublisher {

    void publish(TransferCompletedEvent event);
}