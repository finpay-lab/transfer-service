package com.finpay.transfer.application.saga;

import com.finpay.transfer.domain.event.TransferFailedEvent;

/**
 * Port for emitting a {@code TransferFailed} event. Implementations write the
 * event to the outbox within the same database transaction as the terminal
 * aggregate change (ADR-0004), keeping DB state and event emission atomic.
 */
public interface TransferFailedEventPublisher {

    void publish(TransferFailedEvent event);
}
