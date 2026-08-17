package com.finpay.transfer.application.saga;

import com.finpay.transfer.domain.event.TransferCompletedEvent;

/** Publishes the {@code TransferCompleted} domain event (outbox-backed in infra). */
public interface TransferCompletedEventPublisher {

    void publish(TransferCompletedEvent event);
}
