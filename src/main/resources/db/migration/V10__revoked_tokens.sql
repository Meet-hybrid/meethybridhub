-- ============================================================================
-- V10__revoked_tokens.sql
-- Server-side logout support: a denylist of revoked refresh tokens.
--
-- POST /api/v1/auth/logout hashes the presented refresh token (SHA-256) and
-- stores it here with its original expiry; /refresh rejects any token whose
-- hash is present. Access tokens (24h TTL) are left to expire naturally — no
-- per-request denylist lookup, so the stateless/horizontal-scaling design is
-- preserved.
--
-- Only the token HASH is stored, never the token itself — the table is useless
-- to an attacker even if fully dumped.
-- ============================================================================

CREATE TABLE IF NOT EXISTS revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,        -- SHA-256 hex of the refresh token
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    token_type VARCHAR(20) NOT NULL DEFAULT 'REFRESH',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- when the revoked token would have expired
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Cleanup + lookups
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_tokens(expires_at);

COMMENT ON TABLE revoked_tokens IS 'Denylist of revoked refresh tokens (server-side logout). Only SHA-256 hashes are stored.';
COMMENT ON COLUMN revoked_tokens.token_hash IS 'SHA-256 hex digest of the revoked refresh token (never the token itself).';

-- ============================================================================
-- End of migration
-- ============================================================================
