package com.finpay.transfer.application.saga;

import com.finpay.transfer.domain.event.TransferFailedEvent;

/** Publishes the {@code TransferFailed} domain event (outbox-backed in infra). */
public interface TransferFailedEventPublisher {

    void publish(TransferFailedEvent event);
}
