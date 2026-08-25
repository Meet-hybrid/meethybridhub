-- Payment records and webhook idempotency for order checkout.

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(40) NOT NULL DEFAULT 'KORAPAY',
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'NGN',
    transaction_id VARCHAR(160),
    idempotency_key VARCHAR(160) NOT NULL,
    gateway_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payments_store_idempotency UNIQUE (store_id, idempotency_key),
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'USSD')),
    CONSTRAINT valid_payment_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT payment_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payments_store_order ON payments(store_id, order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_store_transaction
    ON payments(store_id, transaction_id) WHERE transaction_id IS NOT NULL;
