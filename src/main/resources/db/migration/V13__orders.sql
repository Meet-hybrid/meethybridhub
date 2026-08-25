-- Collins order foundation. Orders and line items are scoped to a store.

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    order_number VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(19, 2) NOT NULL,
    shipping_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    customer_email VARCHAR(320) NOT NULL,
    shipping_address TEXT NOT NULL,
    billing_address TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_orders_store_number UNIQUE (store_id, order_number),
    CONSTRAINT valid_order_status CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT order_total_non_negative CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_store_created ON orders(store_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_store_customer ON orders(store_id, customer_id);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT order_item_quantity_positive CHECK (quantity > 0),
    CONSTRAINT order_item_price_non_negative CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_store_order ON order_items(store_id, order_id);
