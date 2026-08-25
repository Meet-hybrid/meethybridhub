-- Installment plans and scheduled payments for eligible customer orders.

CREATE TABLE IF NOT EXISTS installment_plans (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    total_amount NUMERIC(19, 2) NOT NULL,
    installment_count INTEGER NOT NULL,
    installment_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT installment_total_positive CHECK (total_amount > 0),
    CONSTRAINT installment_count_valid CHECK (installment_count BETWEEN 2 AND 12),
    CONSTRAINT installment_amount_positive CHECK (installment_amount > 0),
    CONSTRAINT valid_installment_plan_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DEFAULTED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS installment_payments (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    installment_plan_id BIGINT NOT NULL REFERENCES installment_plans(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_installment_payment_sequence UNIQUE (installment_plan_id, sequence_number),
    CONSTRAINT installment_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT valid_installment_payment_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_installment_plans_store ON installment_plans(store_id);
CREATE INDEX IF NOT EXISTS idx_installment_payments_store_due ON installment_payments(store_id, due_date, status);
