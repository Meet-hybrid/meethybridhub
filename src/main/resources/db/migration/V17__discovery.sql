-- ============================================================================
-- V17__discovery.sql
-- Phase 7: Store/product discovery, reviews, favorites, featured content.
-- ============================================================================

-- store_reviews
-- Customers rate and review stores after completing an order.
CREATE TABLE IF NOT EXISTS store_reviews (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title           VARCHAR(255),
    comment         TEXT,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,  -- true if customer placed an order
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, customer_id)                    -- one review per customer per store
);

CREATE INDEX IF NOT EXISTS idx_store_reviews_store ON store_reviews(store_id, rating);
CREATE INDEX IF NOT EXISTS idx_store_reviews_customer ON store_reviews(customer_id);

COMMENT ON TABLE store_reviews IS 'Customer reviews and ratings for stores (Phase 7).';

-- product_reviews
-- Customers rate and review specific products.
CREATE TABLE IF NOT EXISTS product_reviews (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    store_id        BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title           VARCHAR(255),
    comment         TEXT,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, customer_id)                  -- one review per customer per product
);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product ON product_reviews(product_id, rating);
CREATE INDEX IF NOT EXISTS idx_product_reviews_store ON product_reviews(store_id);

COMMENT ON TABLE product_reviews IS 'Customer reviews and ratings for products (Phase 7).';

-- user_favorites
-- Customers bookmark stores and products they like.
CREATE TABLE IF NOT EXISTS user_favorites (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type     VARCHAR(20) NOT NULL,  -- 'STORE' or 'PRODUCT'
    entity_id       BIGINT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_user_favorites_user ON user_favorites(user_id, entity_type);

COMMENT ON TABLE user_favorites IS 'User bookmarked stores and products (Phase 7).';

-- browsing_history
-- Track what users view for recommendation signals.
CREATE TABLE IF NOT EXISTS browsing_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_id      VARCHAR(100),
    entity_type     VARCHAR(20) NOT NULL,  -- 'STORE' or 'PRODUCT'
    entity_id       BIGINT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_browsing_history_user ON browsing_history(user_id, entity_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_browsing_history_session ON browsing_history(session_id, created_at DESC);

COMMENT ON TABLE browsing_history IS 'Anonymous/authenticated browsing events for recommendations (Phase 7).';

-- featured_content
-- Admin-curated featured stores, products, and banners.
CREATE TABLE IF NOT EXISTS featured_content (
    id              BIGSERIAL PRIMARY KEY,
    content_type    VARCHAR(30) NOT NULL,  -- 'STORE', 'PRODUCT', 'BANNER'
    entity_id       BIGINT,                -- nullable for banners
    title           VARCHAR(255) NOT NULL,
    subtitle        VARCHAR(500),
    image_url       TEXT,
    link_url        TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at       TIMESTAMP WITH TIME ZONE,
    ends_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_content_type CHECK (content_type IN ('STORE', 'PRODUCT', 'BANNER'))
);

CREATE INDEX IF NOT EXISTS idx_featured_content_active ON featured_content(content_type, active, sort_order);

COMMENT ON TABLE featured_content IS 'Admin-curated featured stores, products, and promotional banners (Phase 7).';

-- ============================================================================
-- End of migration
-- ============================================================================
