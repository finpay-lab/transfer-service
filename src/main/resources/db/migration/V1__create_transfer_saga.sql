-- transfer-service schema (owned by this service, ADR-0005 / DATA_OWNERSHIP.md).

-- The transfer saga aggregate. At most one row may exist per idempotency key
-- (AGENTS.md Rule 6); the unique constraint is the concurrency-safe guard.
-- The full saga execution state lives on this row (ADR-0003), so a crash at
-- any point can be resumed deterministically:
--   * saga_step          the step the saga is currently driving
--   * executed_steps     JSONB array of steps whose forward action completed
--   * failure_reason / failed_at_step  surfaced on TransferFailed
CREATE TABLE transfer (
    transfer_id            UUID         PRIMARY KEY,
    customer_id            UUID         NOT NULL,
    source_account_id      UUID         NOT NULL,
    destination_account_id UUID         NOT NULL,
    amount                 DECIMAL(38, 2) NOT NULL,
    currency               VARCHAR(3)   NOT NULL,
    status                 VARCHAR(32)  NOT NULL,
    saga_step              VARCHAR(32)  NOT NULL,
    idempotency_key        VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    failure_reason         VARCHAR(512),
    failed_at_step         VARCHAR(32),
    executed_steps         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    CONSTRAINT uk_transfer_idempotency_key UNIQUE (idempotency_key)
);

-- Crash-recovery query: non-terminal sagas, oldest-first.
CREATE INDEX idx_transfer_recovery ON transfer (status, updated_at);

-- Transactional outbox (ADR-0004): event envelope written atomically with the
-- aggregate; a relay (later phase) publishes unpublished rows to the
-- finpay.transfer topic.
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