-- transfer-service schema (owned by this service, DATA_OWNERSHIP.md).

-- The transfer saga aggregate. At most one row may exist per idempotency key
-- (AGENTS.md Rule 6); the unique constraint is the concurrency-safe guard.
CREATE TABLE transfer (
    transfer_id           UUID         PRIMARY KEY,
    source_account_id     UUID         NOT NULL,
    destination_account_id UUID        NOT NULL,
    amount                DECIMAL(38, 2) NOT NULL,
    currency              CHAR(3)      NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    saga_step             VARCHAR(32)  NOT NULL,
    idempotency_key       VARCHAR(64)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_transfer_idempotency_key UNIQUE (idempotency_key)
);

-- Transactional outbox (ADR-0004): event envelope written atomically with the
-- aggregate; a relay publishes unpublished rows to the finpay.transfer topic.
CREATE TABLE transfer_outbox (
    id             UUID         PRIMARY KEY,
    event_id       UUID         NOT NULL,
    event_type     VARCHAR(64)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    partition_key  VARCHAR(64)  NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- Pending-row lookup for the outbox relay (ADR-0004: index on unpublished set).
CREATE INDEX idx_outbox_pending ON transfer_outbox (created_at) WHERE published_at IS NULL;
