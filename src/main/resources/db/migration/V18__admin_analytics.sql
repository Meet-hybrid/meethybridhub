-- ============================================================================
-- V18__admin_analytics.sql
-- Phase 8: Admin dashboard, analytics, commissions, disputes, platform config.
-- ============================================================================

-- platform_config
-- Key-value store for platform-wide settings (feature flags, thresholds, etc.).
CREATE TABLE IF NOT EXISTS platform_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    description     VARCHAR(500),
    updated_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_platform_config_key ON platform_config(config_key);

COMMENT ON TABLE platform_config IS 'Platform-wide configuration key-value store (Phase 8).';

-- commission_rules
-- Defines commission rates per store or globally.
CREATE TABLE IF NOT EXISTS commission_rules (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT REFERENCES stores(id) ON DELETE CASCADE,  -- NULL = global rule
    rule_type       VARCHAR(30) NOT NULL,  -- 'FLAT_FEE', 'PERCENTAGE', 'TIERED'
    rate            NUMERIC(19,4) NOT NULL,  -- percentage (e.g. 2.5) or flat fee in stroops
    currency        VARCHAR(3) NOT NULL DEFAULT 'NGN',
    min_order       NUMERIC(19,2),          -- minimum order for tiered rules
    max_order       NUMERIC(19,2),          -- maximum order for tiered rules
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_rule_type CHECK (rule_type IN ('FLAT_FEE', 'PERCENTAGE', 'TIERED'))
);

CREATE INDEX IF NOT EXISTS idx_commission_rules_store ON commission_rules(store_id, active);

COMMENT ON TABLE commission_rules IS 'Commission rate rules per store or globally (Phase 8).';

-- commission_entries
-- Records calculated commissions for each completed order.
CREATE TABLE IF NOT EXISTS commission_entries (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    rule_id         BIGINT REFERENCES commission_rules(id) ON DELETE SET NULL,
    order_amount    NUMERIC(19,2) NOT NULL,
    commission_amount NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'NGN',
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING, PAID, DISPUTED, WAIVED
    paid_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_commission_status CHECK (status IN ('PENDING', 'PAID', 'DISPUTED', 'WAIVED'))
);

CREATE INDEX IF NOT EXISTS idx_commission_entries_store ON commission_entries(store_id, status);
CREATE INDEX IF NOT EXISTS idx_commission_entries_order ON commission_entries(order_id);

COMMENT ON TABLE commission_entries IS 'Calculated commission entries for completed orders (Phase 8).';

-- disputes
-- Customer or store owner disputes about orders or commissions.
CREATE TABLE IF NOT EXISTS disputes (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id        BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    commission_id   BIGINT REFERENCES commission_entries(id) ON DELETE SET NULL,
    filed_by_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_to_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    dispute_type    VARCHAR(30) NOT NULL,  -- 'ORDER_QUALITY', 'PAYMENT', 'COMMISSION', 'OTHER'
    subject         VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    -- OPEN, IN_REVIEW, RESOLVED, CLOSED, ESCALATED
    resolution      TEXT,
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    -- LOW, NORMAL, HIGH, URGENT
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_dispute_type CHECK (dispute_type IN ('ORDER_QUALITY', 'PAYMENT', 'COMMISSION', 'OTHER')),
    CONSTRAINT valid_dispute_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED', 'ESCALATED')),
    CONSTRAINT valid_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

CREATE INDEX IF NOT EXISTS idx_disputes_store ON disputes(store_id, status);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status, priority);

COMMENT ON TABLE disputes IS 'Dispute tracking for orders and commissions (Phase 8).';

-- dispute_messages
-- Communication thread within a dispute.
CREATE TABLE IF NOT EXISTS dispute_messages (
    id              BIGSERIAL PRIMARY KEY,
    dispute_id      BIGINT NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    sender_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message         TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dispute_messages_dispute ON dispute_messages(dispute_id, created_at);

COMMENT ON TABLE dispute_messages IS 'Message thread for dispute resolution (Phase 8).';

-- sales_snapshots
-- Daily rollup of sales data per store for fast analytics queries.
CREATE TABLE IF NOT EXISTS sales_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    snapshot_date   DATE NOT NULL,
    order_count     INTEGER NOT NULL DEFAULT 0,
    revenue         NUMERIC(19,2) NOT NULL DEFAULT 0,
    commission      NUMERIC(19,2) NOT NULL DEFAULT 0,
    new_customers   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_sales_snapshots_store_date ON sales_snapshots(store_id, snapshot_date DESC);

COMMENT ON TABLE sales_snapshots IS 'Daily sales rollup per store for analytics (Phase 8).';

-- ============================================================================
-- End of migration
-- ============================================================================
