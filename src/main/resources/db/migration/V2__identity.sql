-- ============================================================================
-- V2__identity.sql
-- Creates the user authentication and authorization tables.
-- This migration is idempotent: it can be run multiple times without error.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users
-- Core user account table for authentication and profile data.
-- Email is unique across the entire platform (multi‑tenant).
-- Roles are stored as a comma‑separated list for simplicity in Phase 2.
-- We'll move to a proper role‑junction table when RBAC complexity increases.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,          -- bcrypt hash, length 60
    full_name VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL DEFAULT 'CUSTOMER', -- e.g., 'CUSTOMER,STORE_OWNER'
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACTIVE, SUSPENDED, DELETED
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Index for email lookups (already unique, but good to be explicit)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Index for status filtering (common for admin queries)
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- ----------------------------------------------------------------------------
-- email_verification_tokens
-- One‑time tokens for verifying email addresses.
-- Tokens expire after 24 hours.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,           -- cryptographically random
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT token_not_expired CHECK (expires_at > created_at)
);

-- Index for token lookups (fast verification)
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens(token);

-- Index for cleaning up expired tokens
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expires ON email_verification_tokens(expires_at);

-- ----------------------------------------------------------------------------
-- password_reset_tokens
-- One‑time tokens for resetting forgotten passwords.
-- Tokens expire after 1 hour (shorter than email verification).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT token_not_expired CHECK (expires_at > created_at)
);

-- Index for token lookups
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);

-- Index for cleanup
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires ON password_reset_tokens(expires_at);

-- ----------------------------------------------------------------------------
-- login_attempts
-- Tracks failed login attempts for rate limiting and account lockout.
-- Records older than 24 hours are cleaned up automatically (cron job).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    failed_reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for rate‑limiting queries (email + time window)
CREATE INDEX IF NOT EXISTS idx_login_attempts_email_created ON login_attempts(email, created_at);

-- Index for security monitoring (IP‑based blocking)
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip_created ON login_attempts(ip_address, created_at);

-- ----------------------------------------------------------------------------
-- audit_log
-- Security‑relevant events for compliance and debugging.
-- We'll expand this in later phases with more event types.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    ip_address INET,
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for time‑based queries (most common audit use case)
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON audit_log(created_at);

-- Index for user‑centric audit views
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id);

-- ----------------------------------------------------------------------------
-- COMMENT on tables and columns for documentation
-- ----------------------------------------------------------------------------
COMMENT ON TABLE users IS 'User accounts for authentication and profile management.';
COMMENT ON COLUMN users.roles IS 'Comma‑separated list of roles: CUSTOMER, STORE_OWNER, ADMIN';
COMMENT ON COLUMN users.status IS 'Account lifecycle state: PENDING (awaiting email verification), ACTIVE, SUSPENDED, DELETED';

COMMENT ON TABLE email_verification_tokens IS 'One‑time tokens for email address verification (24‑hour expiry).';
COMMENT ON TABLE password_reset_tokens IS 'One‑time tokens for password reset (1‑hour expiry).';

COMMENT ON TABLE login_attempts IS 'Track login attempts for rate limiting and security monitoring.';
COMMENT ON COLUMN login_attempts.failed_reason IS 'Reason for failure: WRONG_PASSWORD, ACCOUNT_LOCKED, etc.';

COMMENT ON TABLE audit_log IS 'Security audit trail for compliance (GDPR, SOC2).';
COMMENT ON COLUMN audit_log.metadata IS 'Additional event‑specific data in JSON format.';

-- ============================================================================
-- End of migration
-- ============================================================================