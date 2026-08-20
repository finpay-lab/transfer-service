package com.finpay.transfer.service.infrastructure.messaging;

import com.finpay.transfer.service.infrastructure.persistence.OutboxEntity;
import com.finpay.transfer.service.infrastructure.persistence.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Relay for transfer outbox (FP-13): publish staged events to finpay.transfer. */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka,
                       @org.springframework.beans.factory.annotation.Value("${finpay.transfer.topics.transfer:finpay.transfer}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relay() {
        List<OutboxEntity> pending = outbox.findBySentFalseOrderByCreatedAtAsc();
        for (OutboxEntity e : pending) {
            try {
                kafka.send(topic, e.getAggregateId(), e.getPayload()).get();
                e.setSent(true);
                e.setCreatedAt(Instant.now());
                outbox.save(e);
            } catch (Exception ex) {
                log.error("transfer outbox publish failed {}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}
