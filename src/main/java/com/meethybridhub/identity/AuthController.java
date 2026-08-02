package com.meethybridhub.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller handling user registration, login, and token management.
 *
 * Endpoints:
 *   POST /api/v1/auth/register   - Create new user account
 *   POST /api/v1/auth/login      - Authenticate and get tokens
 *   POST /api/v1/auth/refresh    - Refresh access token
 *   POST /api/v1/auth/logout     - Invalidate refresh token (client-side)
 *   GET  /api/v1/auth/verify     - Verify email with token
 *   POST /api/v1/auth/reset-password - Request password reset
 *   POST /api/v1/auth/reset-password/confirm - Confirm password reset
 *
 * All endpoints are public (no authentication required).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        
        // Generate tokens for immediate login after registration
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(accessToken, refreshToken, "Registration successful. Please verify your email."));
    }

    /**
     * Authenticate user and return JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate tokens
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        // Update user's last login
        userService.recordLogin(userDetails.getUsername());

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Login successful"));
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        // Validate refresh token
        String username = jwtService.extractUsername(request.refreshToken());
        
        if (!jwtService.validateToken(request.refreshToken(), userService.loadUserByUsername(username))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid refresh token"));
        }

        // Generate new tokens
        UserDetails userDetails = userService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, "Token refreshed"));
    }

    /**
     * Verify email with verification token.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    /**
     * Request password reset.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody ResetPasswordRequest request) {
        userService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Password reset email sent if account exists"));
    }

    /**
     * Confirm password reset with token.
     */
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        userService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    // Request/Response records (immutable DTOs)

    public record RegisterRequest(
            String email,
            String password,
            String fullName
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record RefreshTokenRequest(
            String refreshToken
    ) {}

    public record ResetPasswordRequest(
            String email
    ) {}

    public record ConfirmPasswordResetRequest(
            String token,
            String newPassword
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String message
    ) {}
}