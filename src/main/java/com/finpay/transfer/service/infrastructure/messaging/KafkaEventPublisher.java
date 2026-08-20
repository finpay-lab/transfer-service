package com.finpay.transfer.service.infrastructure.messaging;

import com.finpay.transfer.service.domain.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes transfer domain events to the broker (FP-13). */
@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafka;
    public KafkaEventPublisher(KafkaTemplate<String, String> kafka) { this.kafka = kafka; }

    @Override
    public void publish(String topic, String key, String payload) {
        kafka.send(topic, key, payload);
    }
}
