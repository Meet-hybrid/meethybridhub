package com.meethybridhub.common.api;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.RateLimitException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.common.exception.UnauthorizedException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler} — no Spring context.
 *
 * Every handler is a pure "exception → ResponseEntity" translation, so each
 * test constructs the exception directly and asserts the status, the uniform
 * {@link ApiError} envelope, and any headers (e.g. Retry-After).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ApiError body(ResponseEntity<ApiError> response) {
        return response.getBody();
    }

    @Test
    void notFoundMapsTo404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("User 42 not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(response).status()).isEqualTo(404);
        assertThat(body(response).message()).isEqualTo("User 42 not found");
    }

    @Test
    void noResourceFoundMapsTo404IncludingPath() {
        ResponseEntity<ApiError> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/unknown"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(response).message()).isEqualTo("Resource not found: /api/unknown");
    }

    @Test
    void badRequestMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new BadRequestException("Duplicate email"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Duplicate email");
    }

    @Test
    void illegalArgumentMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Weak password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Weak password");
    }

    @Test
    void unauthorizedMapsTo401() {
        ResponseEntity<ApiError> response = handler.handleUnauthorized(
                new UnauthorizedException("Token expired"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).isEqualTo("Token expired");
    }

    @Test
    void badCredentialsMapsTo401WithGenericMessage() {
        // Spring's message may carry internals; the handler must never leak them.
        ResponseEntity<ApiError> response = handler.handleBadCredentials(
                new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).isEqualTo("Invalid email or password");
    }

    @Test
    void disabledAccountMapsTo401() {
        ResponseEntity<ApiError> response = handler.handleDisabled(
                new DisabledException("Account disabled"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).contains("verify your email");
    }

    @Test
    void authenticationMapsTo401WithPrefixedMessage() {
        AuthenticationException ex = mock(AuthenticationException.class);
        when(ex.getMessage()).thenReturn("session expired");

        ResponseEntity<ApiError> response = handler.handleAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).isEqualTo("Authentication failed: session expired");
    }

    @Test
    void malformedJwtMapsTo401WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleMalformedJwt(
                new MalformedJwtException("JWT strings must contain exactly 2 period characters"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).isEqualTo("Invalid token");
    }

    @Test
    void forbiddenMapsTo403() {
        ResponseEntity<ApiError> response = handler.handleForbidden(
                new ForbiddenException("Not your store"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(response).message()).isEqualTo("Not your store");
    }

    @Test
    void rateLimitMapsTo429WithRetryAfterHeader() {
        ResponseEntity<ApiError> response = handler.handleRateLimit(
                new RateLimitException("Too many attempts", 900));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("900");
        assertThat(body(response).message()).isEqualTo("Too many attempts");
        assertThat(body(response).error()).isEqualTo("Too Many Requests");
    }

    @Test
    void validationFailureMapsTo400WithFlattenedDetails() {
        MethodParameter parameter = mock(MethodParameter.class);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "registration");
        binding.addError(new FieldError("registration", "email", "must not be blank"));
        binding.addError(new FieldError("registration", "password", "size must be between 8 and 100"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Validation failed");
        assertThat(body(response).details()).containsExactly(
                "email: must not be blank",
                "password: size must be between 8 and 100");
    }

    @Test
    void unreadableBodyMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("malformed json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Malformed request body");
    }

    @Test
    void typeMismatchMapsTo400WithParameterName() {
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", parameter, new NumberFormatException("abc"));

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Invalid value for parameter: page");
    }

    @Test
    void missingParamMapsTo400WithParameterName() {
        ResponseEntity<ApiError> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("page", "int"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Required parameter is missing: page");
    }

    @Test
    void methodNotSupportedMapsTo405ListingAllowedMethods() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET", "PUT"));

        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(body(response).message())
                .contains("POST")
                .contains("GET")
                .contains("PUT");
    }

    @Test
    void methodNotSupportedWithNullAllowedMethodsMapsTo405() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(body(response).message())
                .isEqualTo("HTTP method DELETE not supported. Allowed methods: ");
    }

    @Test
    void genericExceptionMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ApiError> response = handler.handleGeneric(
                new RuntimeException("db connection string: jdbc:secret://internal"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(response).message()).isEqualTo("An unexpected error occurred");
        // The root cause (including its message) must never reach the client.
        assertThat(body(response).message()).doesNotContain("jdbc:secret://internal");
    }
}
