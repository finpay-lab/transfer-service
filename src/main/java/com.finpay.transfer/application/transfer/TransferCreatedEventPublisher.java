package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.event.TransferCreatedEvent;

/** Publishes the {@code TransferCreated} domain event (outbox-backed in infra). */
public interface TransferCreatedEventPublisher {

    void publish(TransferCreatedEvent event);
}
