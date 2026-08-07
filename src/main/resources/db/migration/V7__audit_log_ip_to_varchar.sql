-- ============================================================================
-- V7__audit_log_ip_to_varchar.sql
-- Swaps audit_log.ip_address from Postgres' INET to VARCHAR(45), matching the
-- login_attempts.ip_address change made in V5.
--
-- Rationale: the AuditLog entity maps the IP as a plain String, which is what
-- the H2 test database supports (H2 has no INET type), and 45 chars covers the
-- longest textual IPv6 address. The table is empty at this point (audit
-- logging ships with this change), so the alter is lossless.
-- ============================================================================

ALTER TABLE audit_log
    ALTER COLUMN ip_address TYPE VARCHAR(45)
    USING ip_address::varchar(45);

COMMENT ON COLUMN audit_log.ip_address IS 'Client IP address (IPv4 or IPv6, up to 45 chars).';

-- ============================================================================
-- End of migration
-- ============================================================================
