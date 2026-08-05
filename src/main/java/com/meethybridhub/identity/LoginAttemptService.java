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
 * Tracks events in the {@code login_attempts} table (purposely separated so the
 * counters never interfere) and enforces windowed limits:
 *
 *   LOGIN (auth attempts, before authentication runs):
 *   1. Per-email lockout — more than {@code max-failed-attempts} FAILED logins
 *      for one email within the window blocks that email until the window passes.
 *   2. Per-IP rate limit — more than {@code max-attempts-per-ip} attempts
 *      (successful or not) from one IP within the window blocks that IP.
 *
 *   EMAIL_SEND (resend-verification / reset-password, before the email goes out):
 *   3. Per-email cap — at most {@code max-emails-per-email} to one address per
 *      window (protects a victim's inbox from flooding).
 *   4. Per-IP cap — at most {@code max-emails-per-ip} from one IP per window
 *      (protects the mail service from bulk abuse).
 *
 * Thresholds come from {@code auth.rate-limit.*} / {@code auth.email-limit.*}
 * (see application.yml) and are intentionally low in the test profile so the
 * behavior is easy to exercise.
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

    @Value("${auth.email-limit.max-per-email:3}")
    private int maxEmailsPerEmail;

    @Value("${auth.email-limit.max-per-ip:10}")
    private int maxEmailsPerIp;

    @Value("${auth.email-limit.window-minutes:15}")
    private int emailWindowMinutes;

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
                .countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
                        email, LoginAttempt.Purpose.LOGIN, false, windowStart);
        if (failedForEmail >= maxFailedAttempts) {
            log.warn("Login rate limit: email {} locked after {} failed attempts", email, failedForEmail);
            throw new RateLimitException(
                    "Too many failed login attempts for this account. Try again in a few minutes.",
                    retryAfterSeconds(windowMinutes));
        }

        long attemptsFromIp = loginAttemptRepository
                .countByIpAddressAndPurposeAndCreatedAtAfter(ip, LoginAttempt.Purpose.LOGIN, windowStart);
        if (attemptsFromIp >= maxAttemptsPerIp) {
            log.warn("Login rate limit: IP {} blocked after {} attempts", ip, attemptsFromIp);
            throw new RateLimitException(
                    "Too many login attempts from this address. Try again in a few minutes.",
                    retryAfterSeconds(windowMinutes));
        }
    }

    /**
     * Record a successful login and RESET the failure counter: the failed
     * LOGIN attempts for this email are cleared, because the user just proved
     * they know the password. Without this, a few old failures could lock out
     * a legitimately-signed-in user.
     */
    public void recordSuccess(String email, String ip, String userAgent) {
        loginAttemptRepository.deleteFailedForEmail(email, LoginAttempt.Purpose.LOGIN);
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, true, null));
    }

    /** Record a failed login with the reason (e.g. BadCredentialsException). */
    public void recordFailure(String email, String ip, String userAgent, String reason) {
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, false, reason));
    }

    /**
     * Enforce the email-flood limits for the email-sending endpoints
     * (resend-verification, reset-password): at most {@code maxEmailsPerEmail}
     * emails to one address and {@code maxEmailsPerIp} from one IP per window.
     * When under the limit, the request is recorded immediately so the NEXT
     * request sees the incremented count. Blocked requests are not recorded.
     *
     * Known tradeoffs (same family as the login lockout):
     *  - An attacker can burn a victim's per-email budget for one window
     *    (delays legit mail by at most {@code emailWindowMinutes}).
     *  - The check-then-record sequence is not atomic, so concurrent requests
     *    can overshoot the cap by a row or two — acceptable for a windowed limit.
     *
     * @throws RateLimitException (HTTP 429) when a limit is exceeded
     */
    public void checkAndRecordEmailSend(String email, String ip, String userAgent) {
        Instant windowStart = Instant.now().minus(emailWindowMinutes, ChronoUnit.MINUTES);

        long sentToEmail = loginAttemptRepository
                .countByEmailAndPurposeAndCreatedAtAfter(email, LoginAttempt.Purpose.EMAIL_SEND, windowStart);
        if (sentToEmail >= maxEmailsPerEmail) {
            log.warn("Email rate limit: address {} limited after {} sends", email, sentToEmail);
            throw new RateLimitException(
                    "Too many emails sent to this address. Try again in a few minutes.",
                    retryAfterSeconds(emailWindowMinutes));
        }

        long sentFromIp = loginAttemptRepository
                .countByIpAddressAndPurposeAndCreatedAtAfter(ip, LoginAttempt.Purpose.EMAIL_SEND, windowStart);
        if (sentFromIp >= maxEmailsPerIp) {
            log.warn("Email rate limit: IP {} limited after {} sends", ip, sentFromIp);
            throw new RateLimitException(
                    "Too many emails requested from this address. Try again in a few minutes.",
                    retryAfterSeconds(emailWindowMinutes));
        }

        loginAttemptRepository.save(new LoginAttempt(
                LoginAttempt.Purpose.EMAIL_SEND, email, ip, userAgent, true, null));
    }

    /** Seconds until the window rolls over — the 429 Retry-After header. */
    private long retryAfterSeconds(int windowMinutes) {
        return windowMinutes * 60L;
    }
}
