-- FP-10/11/12: transfer schema. No shared DB (Rule 1). Saga state persisted for crash-recovery.
CREATE TABLE IF NOT EXISTS transfers (
    transfer_id     VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(72) NOT NULL,
    from_account    VARCHAR(36) NOT NULL,
    to_account      VARCHAR(36) NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    current_step    VARCHAR(16) NOT NULL DEFAULT 'INIT',
    failed          BOOLEAN     NOT NULL DEFAULT FALSE,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_transfers_idem ON transfers (idempotency_key);

-- Rule 6: idempotency for transfer creation.
CREATE TABLE IF NOT EXISTS transfer_idempotency (
    idempotency_key VARCHAR(72) PRIMARY KEY,
    transfer_id     VARCHAR(36) NOT NULL
);

-- Transactional outbox (FP-13 publish; relay delivers to finpay.transfer).
CREATE TABLE IF NOT EXISTS outbox (
    id           VARCHAR(36) PRIMARY KEY,
    event_type   VARCHAR(48) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload      TEXT        NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    sent         BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS ix_transfer_outbox_unsent ON outbox (sent, created_at);
