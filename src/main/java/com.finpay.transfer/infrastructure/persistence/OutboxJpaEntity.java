package com.finpay.transfer.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Outbox row (ADR-0004): the event envelope is stored atomically with the
 * aggregate change. An outbox relay (not yet wired — Kafka is out of scope for
 * this task) later publishes {@code published_at IS NULL} rows to the
 * {@code finpay.transfer} topic and stamps {@code published_at}.
 */
@Entity
@Table(
        name = "transfer_outbox",
        indexes = @Index(name = "idx_outbox_pending", columnList = "created_at"))
public class OutboxJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @Column(name = "event_id", columnDefinition = "uuid", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_id", columnDefinition = "uuid", nullable = false)
    private UUID aggregateId;

    @Column(name = "partition_key", nullable = false, length = 64)
    private String partitionKey;

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxJpaEntity() {
        // JPA
    }

    public OutboxJpaEntity(
            UUID id,
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String partitionKey,
            String payload,
            Instant createdAt,
            Instant publishedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String eventType() {
        return eventType;
    }

    public UUID aggregateId() {
        return aggregateId;
    }

    public String partitionKey() {
        return partitionKey;
    }

    public String payload() {
        return payload;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }
}
