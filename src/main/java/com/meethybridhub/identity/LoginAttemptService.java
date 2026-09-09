package com.meethybridhub.identity;

import com.meethybridhub.common.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


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


    public void recordSuccess(String email, String ip, String userAgent) {
        loginAttemptRepository.deleteFailedForEmail(email, LoginAttempt.Purpose.LOGIN);
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, true, null));
    }


    public void recordFailure(String email, String ip, String userAgent, String reason) {
        loginAttemptRepository.save(new LoginAttempt(email, ip, userAgent, false, reason));
    }


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


    private long retryAfterSeconds(int windowMinutes) {
        return windowMinutes * 60L;
    }
}
