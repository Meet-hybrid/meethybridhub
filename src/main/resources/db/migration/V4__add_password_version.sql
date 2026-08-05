-- ============================================================================
-- V4__add_password_version.sql
-- Adds a monotonically increasing password_version to users.
--
-- JWTs embed this value as a claim ("pwdv") at issuance time. On every request
-- the claim is compared with the user's current version, so all tokens issued
-- before a password reset/change are rejected immediately — closing the
-- "stolen tokens survive a password change" gap of stateless JWTs.
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.password_version IS 'Incremented on every password change; JWTs embedding an older value are rejected.';

-- ============================================================================
-- End of migration
-- ============================================================================
