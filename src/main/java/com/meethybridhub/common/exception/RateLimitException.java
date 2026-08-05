package com.meethybridhub.common.exception;

/**
 * Thrown when a request is rejected because the client exceeded a rate limit
 * (too many failed login attempts, too many requests from one IP, ...).
 * Maps to HTTP 429 Too Many Requests, with a Retry-After header matching the
 * configured limit window.
 */
public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Seconds until the limit window resets — emitted as the Retry-After header. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
