package com.meethybridhub.common.api;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by {@link GlobalExceptionHandler}.
 *
 * A consistent error contract is a contract with every frontend and API consumer:
 * they parse ONE shape, never guess. Real APIs (Stripe, GitHub) do exactly this.
 *
 * A Java {@code record} is ideal here: it is immutable, and the compiler
 * generates the constructor, accessors, equals/hashCode/toString for free —
 * less code to write, and no mutable state to corrupt.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details) {

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }

    public static ApiError of(int status, String error, String message) {
        return of(status, error, message, List.of());
    }
}
