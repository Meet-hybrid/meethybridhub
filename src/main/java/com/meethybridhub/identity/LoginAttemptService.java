package com.meethybridhub.identity;

import com.meethybridhub.common.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tracks login attempts in the {@code login_attempts} table and enforces two
 * windowed limits before authentication runs:
 *
 *   1. Per-email lockout — more than {@code max-failed-attempts} FAILED logins
 *      for one email within the window blocks that email until the window passes.
 *   2. Per-IP rate limit — more than {@code max-attempts-per-ip} attempts
 *      (successful or not) from one IP within the window blocks that IP.
 *
 * Thresholds come from {@code auth.rate-limit.*} (see application.yml) and are
 * intentionally low in the test profile so the behavior is easy to exercise.
 *
 * KNOWN TRADEOFF: per-email lockout is a mild DoS surface — anyone can lock a
 * known address out for a window by typing the wrong password. That is the
 * standard cost of lockout (and the window is short); if it becomes a problem,
 * key the lockout on the email+IP pair instead of the email alone.
 */
@Service
@Transactional
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${auth.rate-limit.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.rate-limit.max-attempts-per-ip:20}")
    private int maxAttemptsPerIp;

    @Value("${auth.rate-limit.window-minutes:15}")
    private int windowMinutes;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    /**
     * Reject the request if the email is locked out or the IP is rate-limited.
     *
     * @throws RateLimitException (HTTP 429) when a limit is exceeded
     */
    public void checkRateLimit(String email, String ip) {
        Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);

        long failedForEmail = loginAttemptRepository
                .countByEmailAndSuccessAndCreatedAtAfter(email, false, windowStart);
        if (failedForEmail >= maxFailedAttempts) {
            log.warn("Login rate limit: email {} locked after {} failed attempts", email, failedForEmail);
            throw new RateLimitException(
                    "Too many failed login attempts for this account. Try again in a few minutes.",
                    retryAfterSeconds());
        }

        long attemptsFromIp = loginAttemptRepository
                .countByIpAddressAndCreatedAtAfter(ip, windowStart);
        if (attemptsFromIp >= maxAttemptsPerIp) {
            log.warn("Login rate limit: IP {} blocked after {} attempts", ip, attemptsFromIp);
            throw new RateLimitException(
                    "Too many login attempts from this address. Try again in a few minutes.",
                    retryAfterSeconds());
        }
    }

    /**
     * Record a successful login and RESET the failure counter: the failed
     * attempts for this email are cleared, because the user just proved they
     * know the password. Without this, a few old failures could lock out a
     * legitimately-signed-in user.
     */
    public void recordSuccess(String email, String ip, String userAgent) {
        loginAttemptRepository.deleteFailedForEmail(email);
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, true, null));
    }

    /** Record a failed login with the reason (e.g. BadCredentialsException). */
    public void recordFailure(String email, String ip, String userAgent, String reason) {
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, false, reason));
    }

    /** Seconds until the window rolls over — the 429 Retry-After header. */
    private long retryAfterSeconds() {
        return windowMinutes * 60L;
    }
}
