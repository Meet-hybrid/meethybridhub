package com.meethybridhub.common.exception;

/**
 * Thrown when a requested resource does not exist (maps to HTTP 404).
 *
 * Keeping a small set of explicit domain exceptions (instead of throwing
 * generic RuntimeException everywhere) gives us:
 *   - intention-revealing code (a reader knows what went wrong),
 *   - a single place (GlobalExceptionHandler) that decides the HTTP mapping.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
