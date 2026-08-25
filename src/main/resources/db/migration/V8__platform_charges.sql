-- ============================================================================
-- V8__platform_charges.sql
-- Dormant platform-charge module (see docs/platform-charge-dormant-module.md).
--
-- Records a flat platform fee per chargeable transaction. The module ships
-- ASLEEP (PLATFORM_CHARGE_ENABLED=false) — no rows are written until it is
-- activated, so this table stays empty until the Orders/Payments phase lands
-- and a ChargeableTransactionSource is registered.
-- ============================================================================

CREATE TABLE IF NOT EXISTS platform_charges (
    id BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(100) NOT NULL UNIQUE,   -- idempotency key (one charge per transaction)
    transaction_amount NUMERIC(12,2) NOT NULL,       -- the transaction's settled amount
    charge_amount NUMERIC(12,2) NOT NULL,            -- the flat fee charged on it
    currency VARCHAR(3) NOT NULL DEFAULT 'NGN',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, COLLECTED, FAILED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Idempotency + sweep lookups
CREATE INDEX IF NOT EXISTS idx_platform_charges_status ON platform_charges(status);
CREATE INDEX IF NOT EXISTS idx_platform_charges_created ON platform_charges(created_at);

COMMENT ON TABLE platform_charges IS 'Flat platform fee per transaction (dormant module — see docs/platform-charge-dormant-module.md).';
COMMENT ON COLUMN platform_charges.transaction_ref IS 'Unique reference of the charged transaction; the idempotency key.';
COMMENT ON COLUMN platform_charges.status IS 'Lifecycle: PENDING (recorded, not yet collected), COLLECTED, FAILED.';

-- ============================================================================
-- End of migration
-- ============================================================================
