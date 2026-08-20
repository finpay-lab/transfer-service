package com.finpay.transfer.service.domain;

/** Publishes domain events to the broker (FP-13). */
public interface EventPublisher {
    void publish(String topic, String key, String payload);
}
