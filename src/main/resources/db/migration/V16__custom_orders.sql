-- ============================================================================
-- V16__custom_orders.sql
-- Phase 6: Custom order request system with quotes and communication.
-- ============================================================================

-- custom_order_requests
-- A customer submits a request describing what they want made.
CREATE TABLE IF NOT EXISTS custom_order_requests (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    -- OPEN, IN_REVIEW, QUOTED, ACCEPTED, REJECTED, EXPIRED, CONVERTED
    budget_min      NUMERIC(19,2),
    budget_max      NUMERIC(19,2),
    deadline        TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_cor_status CHECK (status IN (
        'OPEN','IN_REVIEW','QUOTED','ACCEPTED','REJECTED','EXPIRED','CONVERTED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_cor_store ON custom_order_requests(store_id, status);
CREATE INDEX IF NOT EXISTS idx_cor_customer ON custom_order_requests(customer_id);

COMMENT ON TABLE custom_order_requests IS 'Customer-initiated custom order requests (Phase 6).';

-- quotes
-- Store owner responds to a request with a price and terms.
CREATE TABLE IF NOT EXISTS quotes (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES custom_order_requests(id) ON DELETE CASCADE,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    price           NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'NGN',
    estimated_days  INTEGER,
    terms           TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- PENDING, ACCEPTED, REJECTED, EXPIRED, WITHDRAWN
    expires_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_quote_status CHECK (status IN (
        'PENDING','ACCEPTED','REJECTED','EXPIRED','WITHDRAWN'
    ))
);

CREATE INDEX IF NOT EXISTS idx_quotes_request ON quotes(request_id);
CREATE INDEX IF NOT EXISTS idx_quotes_store ON quotes(store_id, status);

COMMENT ON TABLE quotes IS 'Store-owner price quotes for custom order requests.';

-- custom_order_conversations
-- Message thread between customer and store for a given request.
CREATE TABLE IF NOT EXISTS custom_order_conversations (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES custom_order_requests(id) ON DELETE CASCADE,
    sender_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message         TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coc_request ON custom_order_conversations(request_id, created_at);

COMMENT ON TABLE custom_order_conversations IS 'Message thread for a custom order request.';

-- custom_order_attachments
-- Reference images / files attached to a request or conversation message.
CREATE TABLE IF NOT EXISTS custom_order_attachments (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES custom_order_requests(id) ON DELETE CASCADE,
    conversation_id BIGINT REFERENCES custom_order_conversations(id) ON DELETE SET NULL,
    uploader_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_url        TEXT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(100),
    file_size_bytes BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coa_request ON custom_order_attachments(request_id);

COMMENT ON TABLE custom_order_attachments IS 'Files attached to custom order requests or messages.';

-- ============================================================================
-- End of migration
-- ============================================================================
