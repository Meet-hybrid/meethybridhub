package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);
    private static final int LOGIN_ATTEMPT_RETENTION_HOURS = 24;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    public TokenCleanupService(EmailVerificationTokenRepository emailVerificationTokenRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               LoginAttemptRepository loginAttemptRepository,
                               RevokedTokenRepository revokedTokenRepository) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.revokedTokenRepository = revokedTokenRepository;
    }


    @Scheduled(cron = "${app.token-cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();

        int expiredVerification = emailVerificationTokenRepository.deleteExpired(now);
        int expiredReset = passwordResetTokenRepository.deleteExpired(now);
        long expiredRevoked = revokedTokenRepository.deleteByExpiresAtBefore(now);
        int staleLoginAttempts = loginAttemptRepository.deleteBefore(
                now.minus(LOGIN_ATTEMPT_RETENTION_HOURS, ChronoUnit.HOURS));

        long total = expiredVerification + expiredReset + expiredRevoked + staleLoginAttempts;
        if (total > 0) {
            log.info("Cleanup: deleted {} expired verification tokens, {} expired reset tokens, "
                            + "{} expired revoked tokens, {} stale login attempts",
                    expiredVerification, expiredReset, expiredRevoked, staleLoginAttempts);
        }
    }
}
