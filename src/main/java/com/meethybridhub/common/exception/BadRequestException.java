package com.meethybridhub.common.exception;

/**
 * Thrown when a request is understood but invalid — e.g. a business rule is
 * violated (maps to HTTP 400).
 *
 * Business-rule violations belong in the SERVICE layer, not the controller:
 * the controller only knows "how to receive requests", the service knows
 * "what the business allows".
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
