-- ============================================================================
-- V3__stores.sql
-- Multi-tenancy: the `stores` table plus store-scoped `store_domains`.
--
-- Strategy (see docs/MeethybridHub_Project_Documentation.md):
--   shared PostgreSQL schema with a `store_id` column on every tenant-scoped
--   table, composite indexes with store_id as the leading column, and
--   row-level isolation enforced in application logic (StoreFilter +
--   TenantContext + tenant-aware repositories).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- stores
-- The tenant itself. Each store belongs to exactly one owner (a platform user).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stores (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,          -- also used as the store subdomain
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, SUSPENDED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_store_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

-- Store owner lookups (authz: "which store does this user own?")
CREATE INDEX IF NOT EXISTS idx_stores_owner ON stores(owner_user_id);

-- Subdomain/tenant resolution lookups
CREATE INDEX IF NOT EXISTS idx_stores_slug ON stores(slug);

-- ----------------------------------------------------------------------------
-- store_domains
-- Maps domain names (custom or subdomain) to a store. Store-scoped: carries
-- store_id and is the reference implementation of the TenantEntity pattern.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store_domains (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,    -- DNS verification (Phase 3 week 4)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Composite index with store_id as leading column (documented strategy)
CREATE INDEX IF NOT EXISTS idx_store_domains_store ON store_domains(store_id, is_primary);

-- Tenant resolution from a Host header
CREATE INDEX IF NOT EXISTS idx_store_domains_domain ON store_domains(domain);

-- ----------------------------------------------------------------------------
-- COMMENT on tables for documentation
-- ----------------------------------------------------------------------------
COMMENT ON TABLE stores IS 'Tenant stores. Every store-scoped table carries a store_id column (shared-schema multi-tenancy).';
COMMENT ON COLUMN stores.slug IS 'URL/subdomain-friendly unique identifier, e.g. divine-signature';
COMMENT ON COLUMN stores.status IS 'Lifecycle: PENDING, ACTIVE, SUSPENDED';
COMMENT ON TABLE store_domains IS 'Domains/subdomains mapped to a store (e.g. divine-signature.meethybridhub.com).';

-- ============================================================================
-- End of migration
-- ============================================================================
