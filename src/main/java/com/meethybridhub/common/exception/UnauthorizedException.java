package com.meethybridhub.common.exception;

/**
 * Thrown when authentication fails or user lacks proper credentials.
 * Maps to HTTP 401 Unauthorized.
 *
 * Use this for:
 *   - Invalid login credentials
 *   - Expired/malformed JWT tokens
 *   - Missing authentication tokens
 *   - User account disabled/suspended
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}