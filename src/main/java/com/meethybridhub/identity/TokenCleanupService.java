package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Daily housekeeping job: purges expired one-time tokens and login attempts
 * older than 24 hours (per the V2__identity.sql comment).
 *
 * Runs daily at 3 AM by default (override with the {@code app.token-cleanup.cron}
 * property). Idempotent and safe to run on every instance of a deployment.
 */
@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);
    private static final int LOGIN_ATTEMPT_RETENTION_HOURS = 24;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    public TokenCleanupService(EmailVerificationTokenRepository emailVerificationTokenRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               LoginAttemptRepository loginAttemptRepository) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
    }

    /**
     * Purge expired tokens and stale login attempts.
     */
    @Scheduled(cron = "${app.token-cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();

        int expiredVerification = emailVerificationTokenRepository.deleteExpired(now);
        int expiredReset = passwordResetTokenRepository.deleteExpired(now);
        int staleLoginAttempts = loginAttemptRepository.deleteBefore(
                now.minus(LOGIN_ATTEMPT_RETENTION_HOURS, ChronoUnit.HOURS));

        int total = expiredVerification + expiredReset + staleLoginAttempts;
        if (total > 0) {
            log.info("Cleanup: deleted {} expired verification tokens, {} expired reset tokens, "
                            + "{} stale login attempts",
                    expiredVerification, expiredReset, staleLoginAttempts);
        }
    }
}
