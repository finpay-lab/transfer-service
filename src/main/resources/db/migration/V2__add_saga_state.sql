-- transfer-service schema v2 (FP-12): persisted saga state for crash recovery.

-- The full saga execution state is persisted on the aggregate (ADR-0003), so a
-- crash at any point can be resumed deterministically:
--   * updated_at        last state transition (used to order recovery work)
--   * reservation_id    wallet reservation created by the RESERVATION step
--   * compensating      true once the saga entered the compensation path
--   * executed_steps    JSONB array of steps whose forward action completed
--   * compensated_steps JSONB array of steps already compensated (idempotent
--                       compensation keyed by (transfer_id, step))
--   * failure_reason / failed_at_step  surfaced on TransferFailed
ALTER TABLE transfer
    ADD COLUMN reservation_id    UUID,
    ADD COLUMN failure_reason    VARCHAR(512),
    ADD COLUMN failed_at_step    VARCHAR(32),
    ADD COLUMN compensating      BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN executed_steps    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN compensated_steps JSONB        NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now();

-- Crash-recovery query: non-terminal sagas, oldest-first.
CREATE INDEX idx_transfer_recovery ON transfer (status, updated_at);
