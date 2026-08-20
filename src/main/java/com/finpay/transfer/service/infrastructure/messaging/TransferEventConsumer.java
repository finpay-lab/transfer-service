package com.finpay.transfer.service.infrastructure.messaging;

import com.finpay.transfer.service.domain.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes finpay.transfer events (FP-13). Idempotent by eventId (Rule 7):
 * duplicate/out-of-order delivery is tolerated via an in-memory seen-set
 * (a prod system would use the DB outbox idempotency table keyed by eventId).
 */
@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);
    private final TransferRepository repository;
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();

    public TransferEventConsumer(TransferRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "finpay.transfer", groupId = "transfer-consumer")
    public void onEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
                        Acknowledgment ack) {
        String eventId = record.key();
        if (eventId != null && seen.putIfAbsent(eventId, Boolean.TRUE) != null) {
            log.info("duplicate event {} skipped (idempotent)", eventId);
            ack.acknowledge();
            return;
        }
        log.info("consumed transfer event {}: {}", eventId, record.value());
        ack.acknowledge();
    }
}
