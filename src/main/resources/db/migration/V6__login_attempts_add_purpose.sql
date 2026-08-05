-- ============================================================================
-- V6__login_attempts_add_purpose.sql
-- Distinguishes LOGIN attempts from EMAIL_SEND events in login_attempts.
--
-- The email-flood limiter (resend-verification / reset-password) shares this
-- table so both counters keep the same 24h retention, but every windowed
-- count is purpose-filtered — email sends can never consume the login
-- lockout/IP budget and vice versa. Existing rows default to 'LOGIN'.
-- ============================================================================

ALTER TABLE login_attempts
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'LOGIN';

COMMENT ON COLUMN login_attempts.purpose IS 'Event kind: LOGIN (auth attempts) or EMAIL_SEND (verification/reset emails).';

-- ============================================================================
-- End of migration
-- ============================================================================
