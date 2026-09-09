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


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {


        return build(HttpStatus.NOT_FOUND, "Resource not found: " + ex.getResourcePath());
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }


    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Account is disabled. Please verify your email or contact support.");
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage());
    }


    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiError> handleMalformedJwt(MalformedJwtException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid token");
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }


    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(), ex.getMessage()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName());
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required parameter is missing: " + ex.getParameterName());
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().toString();
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " not supported. Allowed methods: " + allowed);
    }


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
