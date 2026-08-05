-- ============================================================================
-- V5__login_attempts_ip_to_varchar.sql
-- Shapes the login_attempts table for the entity used by rate limiting:
--
--   1. Swaps ip_address from Postgres' INET to VARCHAR(45) — the entity maps
--      the IP as a plain String, which is what the H2 test database supports
--      (H2 has no INET type); 45 chars covers the longest textual IPv6 address.
--   2. Adds purpose — distinguishes LOGIN attempts from EMAIL_SEND events
--      (verification resends / password-reset requests share this table so the
--      two counters never interfere and both keep the same 24h retention).
--
-- The table is empty at this point (login tracking ships with this change),
-- so the alters are lossless.
-- ============================================================================

ALTER TABLE login_attempts
    ALTER COLUMN ip_address TYPE VARCHAR(45)
    USING ip_address::varchar(45);

ALTER TABLE login_attempts
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'LOGIN';

COMMENT ON COLUMN login_attempts.ip_address IS 'Client IP address (IPv4 or IPv6, up to 45 chars).';
COMMENT ON COLUMN login_attempts.purpose IS 'Event kind: LOGIN (auth attempts) or EMAIL_SEND (verification/reset emails).';

-- ============================================================================
-- End of migration
-- ============================================================================
