-- ============================================================================
-- V9__store_settings.sql
-- Store settings & branding (Phase 3, Week 4 of the development plan).
--
-- One row per store (1:1). Holds the storefront branding: logo, brand colors,
-- theme, tagline, and a public contact address. Store-scoped like every other
-- tenant table: carries store_id and is only reachable through the owner's own
-- tenant context (StoreService.getCurrentTenantStore).
-- ============================================================================

CREATE TABLE IF NOT EXISTS store_settings (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    logo_url VARCHAR(500),
    primary_color VARCHAR(7) NOT NULL DEFAULT '#111111',   -- #RRGGBB
    accent_color VARCHAR(7) NOT NULL DEFAULT '#0d9488',    -- #RRGGBB
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',            -- LIGHT, DARK
    tagline VARCHAR(200),
    contact_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_store_settings_store UNIQUE (store_id)
);

CREATE INDEX IF NOT EXISTS idx_store_settings_store ON store_settings(store_id);

COMMENT ON TABLE store_settings IS 'Storefront branding and settings (1:1 with stores).';
COMMENT ON COLUMN store_settings.primary_color IS 'Primary brand color as #RRGGBB.';
COMMENT ON COLUMN store_settings.accent_color IS 'Accent brand color as #RRGGBB.';
COMMENT ON COLUMN store_settings.theme IS 'Storefront theme: LIGHT or DARK.';
COMMENT ON COLUMN store_settings.logo_url IS 'URL of the store logo asset.';

-- ============================================================================
-- End of migration
-- ============================================================================
