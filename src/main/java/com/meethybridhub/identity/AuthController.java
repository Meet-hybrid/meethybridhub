package com.meethybridhub.identity;

import com.meethybridhub.common.exception.UnauthorizedException;
import com.meethybridhub.identity.validation.ValidEmail;
import com.meethybridhub.identity.validation.ValidPassword;
import com.meethybridhub.store.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    // NOTE: identity -> store dependency is intentional (tokens carry the
    // owner's store claim). No bean cycle: StoreService never depends on this
    // controller.
    private final StoreService storeService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService,
            StoreService storeService,
            LoginAttemptService loginAttemptService,
            AuditLogService auditLogService,
            ClientIpResolver clientIpResolver) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.storeService = storeService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        
        // Generate tokens for immediate login after registration
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = tenantClaims(user.getId());
        String accessToken = jwtService.generateAccessToken(userDetails, claims);
        String refreshToken = jwtService.generateRefreshToken(userDetails, claims);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(accessToken, refreshToken, "Registration successful. Please verify your email."));
    }

    /**
     * Authenticate user and return JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Enforce per-email lockout and per-IP rate limit BEFORE authenticating.
        loginAttemptService.checkRateLimit(request.email(), ip);

        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate tokens
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Map<String, Object> claims = tenantClaims(((AppUser) userDetails).getUser().getId());
            String accessToken = jwtService.generateAccessToken(userDetails, claims);
            String refreshToken = jwtService.generateRefreshToken(userDetails, claims);
            
            // Update user's last login and record the successful attempt
            userService.recordLogin(userDetails.getUsername());
            loginAttemptService.recordSuccess(request.email(), ip, userAgent);
            auditLogService.record(((AppUser) userDetails).getUser().getId(),
                    AuditEventType.LOGIN_SUCCESS, "Login successful", ip, userAgent);

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Login successful"));
        } catch (AuthenticationException e) {
            // Record the failure (feeds the lockout counter + audit trail), then
            // convert to our custom exception.
            loginAttemptService.recordFailure(request.email(), ip, userAgent, e.getClass().getSimpleName());
            auditLogService.record(null, AuditEventType.LOGIN_FAILED,
                    "Failed login attempt for " + request.email() + " (" + e.getClass().getSimpleName() + ")",
                    ip, userAgent);
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            // Validate refresh token (signature, expiry, AND that the password
            // hasn't changed since it was issued)
            String username = jwtService.extractUsername(request.refreshToken());
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (!jwtService.validateToken(request.refreshToken(), userDetails)
                    || !jwtService.passwordVersionMatches(request.refreshToken(), userDetails)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            // Generate new tokens (re-deriving the store claim in case the user
            // created a store since the refresh token was issued)
            Map<String, Object> claims = tenantClaims(((AppUser) userDetails).getUser().getId());
            String newAccessToken = jwtService.generateAccessToken(userDetails, claims);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails, claims);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, "Token refreshed"));
        } catch (Exception e) {
            // Catch any JWT parsing errors
            throw new UnauthorizedException("Invalid refresh token");
        }
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
     * Re-send the verification email (e.g. the original link expired).
     * Always returns the same success message, even for unknown or
     * already-verified addresses, to avoid leaking which emails are registered.
     */
    /**
     * Re-send the verification email (e.g. the original link expired).
     * Always returns the same success message, even for unknown or
     * already-verified addresses, to avoid leaking which emails are registered.
     *
     * Rate-limited (per address and per IP) to stop an attacker from flooding
     * a victim's inbox with verification emails.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        loginAttemptService.checkAndRecordEmailSend(
                request.email(), clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        userService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Verification email sent if the account exists and is not yet verified"));
    }

    /**
     * Request password reset.
     * Always returns success even if the account doesn't exist (no enumeration).
     *
     * Rate-limited (per address and per IP) to stop an attacker from flooding
     * a victim's inbox with reset emails.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        loginAttemptService.checkAndRecordEmailSend(
                request.email(), clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
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
            @ValidEmail
            String email,
            
            @ValidPassword
            String password,
            
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName
    ) {}

    public record LoginRequest(
            @ValidEmail
            String email,
            
            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record ResetPasswordRequest(
            @ValidEmail
            String email
    ) {}

    public record ResendVerificationRequest(
            @ValidEmail
            String email
    ) {}

    public record ConfirmPasswordResetRequest(
            @NotBlank(message = "Reset token is required")
            String token,
            
            @ValidPassword
            String newPassword
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String message
    ) {}

    /**
     * Claims that pin a token to the user's store: the ID of the active store
     * they own, when they own one. StoreFilter later reads this to resolve the
     * tenant for store-owner dashboards without headers or subdomains.
     */
    private Map<String, Object> tenantClaims(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        storeService.findActiveStoreIdForOwner(userId)
                .ifPresent(storeId -> claims.put(JwtService.CLAIM_STORE_ID, storeId));
        return claims;
    }
}