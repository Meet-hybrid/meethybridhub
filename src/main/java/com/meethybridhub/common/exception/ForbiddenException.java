package com.meethybridhub.common.exception;

/**
 * Thrown when a user is authenticated but lacks permission for the requested resource.
 * Maps to HTTP 403 Forbidden.
 *
 * Use this for:
 *   - User tries to access admin-only endpoints
 *   - Store owner tries to access another store's data
 *   - Role-based access control violations
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}