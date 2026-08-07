package com.meethybridhub.common.api;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.RateLimitException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.common.exception.UnauthorizedException;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Translates exceptions thrown anywhere in the application into the uniform
 * {@link ApiError} JSON envelope.
 *
 * A classic beginner mistake is ONLY a blanket @ExceptionHandler(Exception.class):
 * since Spring Framework 6.1, requests to unknown URLs throw
 * {@link NoResourceFoundException}, which such a catch-all silently converts into
 * a 500 — so a client probing a wrong path makes monitoring look like the service
 * is down. The explicit handlers below preserve the correct HTTP semantics:
 *
 *   404 — unknown resource, unknown URL
 *   400 — malformed body, failed bean validation, bad/missing parameters
 *   401 — authentication failed
 *   403 — authenticated but unauthorized
 *   405 — wrong HTTP method for a known URL
 *   500 — only the truly unexpected (full stack trace logged, never leaked)
 *
 * @RestControllerAdvice = a global @ControllerAdvice that applies to all
 * controllers and writes response bodies directly (no view resolution).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 — the requested resource does not exist. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * 404 — a request for a URL that matches no controller and no static resource.
     * Without this handler, Spring 6.1+'s NoResourceFoundException falls into the
     * generic catch-all and is wrongly reported as a 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        // Including the path turns a generic 404 into something a support agent
        // (or a frontend dev) can act on without digging through logs.
        return build(HttpStatus.NOT_FOUND, "Resource not found: " + ex.getResourcePath());
    }

    /** 400 — the request is understood but violates a business rule. */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 400 — business-rule violations surfaced as IllegalArgumentException
     * (e.g. duplicate email on registration, weak password). Without this,
     * these fall into the generic handler and wrongly become 500s.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 401 — authentication failed. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** 401 — Spring Security authentication failed (bad credentials). */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /** 401 — User account is disabled/suspended. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Account is disabled. Please verify your email or contact support.");
    }

    /** 401 — Generic Spring Security authentication exception. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage());
    }

    /** 401 — Malformed JWT token. */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiError> handleMalformedJwt(MalformedJwtException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid token");
    }

    /** 403 — authenticated but unauthorized. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * 403 — Spring Security denied the request (e.g. an {@code @PreAuthorize}
     * rule failed). Without this handler it falls into the generic catch-all
     * and wrongly becomes a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    /**
     * 429 — the client exceeded a rate limit. Retry-After tells the caller how
     * long until the limit window resets (derived from the configured window,
     * e.g. 900s for the default 15 minutes).
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(), ex.getMessage()));
    }

    /**
     * 400 — bean validation (@Valid) failed. Field-level errors are flattened
     * into readable strings: "email: must not be blank".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /** 400 — request body was malformed JSON (or unparseable). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    /** 400 — a parameter of the wrong type, e.g. GET /products?page=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName());
    }

    /** 400 — a required query parameter is absent. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required parameter is missing: " + ex.getParameterName());
    }

    /** 405 — the URL exists but not for this HTTP method (e.g. POST to a GET-only URL). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().toString();
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " not supported. Allowed methods: " + allowed);
    }

    /**
     * 500 — the last line of defence. Log the FULL stack trace server-side
     * (that is our debugging source of truth), but never leak it to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, details));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return build(status, message, List.of());
    }
}
