package com.meethybridhub.identity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link TokenCleanupService#purgeExpiredTokens()} removes only
 * expired tokens (the scheduled trigger itself is disabled in the test profile,
 * so the method is exercised directly).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenCleanupServiceTest {

    @Autowired
    private TokenCleanupService tokenCleanupService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void purgeExpiredTokensDeletesOnlyExpiredOnes() {
        User user = userRepository.save(new User(
                "cleanup@example.com", passwordEncoder.encode("TestPassword123!"), "Cleanup User"));

        String expiredVerification = "a".repeat(64);
        String validVerification = "b".repeat(64);
        emailVerificationTokenRepository.save(new EmailVerificationToken(
                user.getId(), expiredVerification, Instant.now().minus(1, ChronoUnit.HOURS)));
        emailVerificationTokenRepository.save(new EmailVerificationToken(
                user.getId(), validVerification, Instant.now().plus(1, ChronoUnit.DAYS)));

        String expiredReset = "c".repeat(64);
        String validReset = "d".repeat(64);
        passwordResetTokenRepository.save(new PasswordResetToken(
                user.getId(), expiredReset, Instant.now().minus(1, ChronoUnit.HOURS)));
        passwordResetTokenRepository.save(new PasswordResetToken(
                user.getId(), validReset, Instant.now().plus(1, ChronoUnit.DAYS)));

        tokenCleanupService.purgeExpiredTokens();

        // Expired tokens are gone, valid ones remain
        assertThat(emailVerificationTokenRepository.findByToken(expiredVerification)).isEmpty();
        assertThat(emailVerificationTokenRepository.findByToken(validVerification)).isPresent();
        assertThat(passwordResetTokenRepository.findByToken(expiredReset)).isEmpty();
        assertThat(passwordResetTokenRepository.findByToken(validReset)).isPresent();

        assertThat(emailVerificationTokenRepository.count()).isEqualTo(1);
        assertThat(passwordResetTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void purgeRemovesStaleLoginAttemptsButKeepsRecentOnes() {
        loginAttemptRepository.save(new LoginAttempt(
                "old@example.com", "127.0.0.1", "test-agent", false, "BadCredentialsException"));
        loginAttemptRepository.save(new LoginAttempt(
                "recent@example.com", "127.0.0.1", "test-agent", false, "BadCredentialsException"));

        // Age the old row by 25 hours (created_at is audited, so use a native update)
        entityManager.createNativeQuery(
                "UPDATE login_attempts SET created_at = DATEADD('HOUR', -25, CURRENT_TIMESTAMP) "
                        + "WHERE email = 'old@example.com'")
                .executeUpdate();

        tokenCleanupService.purgeExpiredTokens();

        Instant dayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        assertThat(loginAttemptRepository.countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
                "old@example.com", LoginAttempt.Purpose.LOGIN, false, dayAgo)).isZero();
        assertThat(loginAttemptRepository.countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
                "recent@example.com", LoginAttempt.Purpose.LOGIN, false, dayAgo)).isEqualTo(1);
    }

    @Test
    void purgeExpiredTokensIsIdempotent() {
        User user = userRepository.save(new User(
                "cleanup-2@example.com", passwordEncoder.encode("TestPassword123!"), "Cleanup User 2"));
        emailVerificationTokenRepository.save(new EmailVerificationToken(
                user.getId(), "e".repeat(64), Instant.now().minus(2, ChronoUnit.HOURS)));

        tokenCleanupService.purgeExpiredTokens();
        tokenCleanupService.purgeExpiredTokens(); // second run must not fail

        assertThat(emailVerificationTokenRepository.count()).isZero();
    }
}
