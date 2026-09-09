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


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;


    private final StoreService storeService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final ClientIpResolver clientIpResolver;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService,
            StoreService storeService,
            LoginAttemptService loginAttemptService,
            AuditLogService auditLogService,
            ClientIpResolver clientIpResolver,
            TokenRevocationService tokenRevocationService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.storeService = storeService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
        this.clientIpResolver = clientIpResolver;
        this.tokenRevocationService = tokenRevocationService;
    }


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);


        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = tenantClaims(user.getId());
        String accessToken = jwtService.generateAccessToken(userDetails, claims);
        String refreshToken = jwtService.generateRefreshToken(userDetails, claims);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(accessToken, refreshToken, "Registration successful. Please verify your email."));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");


        loginAttemptService.checkRateLimit(request.email(), ip);

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);


            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Map<String, Object> claims = tenantClaims(((AppUser) userDetails).getUser().getId());
            String accessToken = jwtService.generateAccessToken(userDetails, claims);
            String refreshToken = jwtService.generateRefreshToken(userDetails, claims);


            userService.recordLogin(userDetails.getUsername());
            loginAttemptService.recordSuccess(request.email(), ip, userAgent);
            auditLogService.record(((AppUser) userDetails).getUser().getId(),
                    AuditEventType.LOGIN_SUCCESS, "Login successful", ip, userAgent);

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Login successful"));
        } catch (AuthenticationException e) {


            loginAttemptService.recordFailure(request.email(), ip, userAgent, e.getClass().getSimpleName());
            auditLogService.record(null, AuditEventType.LOGIN_FAILED,
                    "Failed login attempt for " + request.email() + " (" + e.getClass().getSimpleName() + ")",
                    ip, userAgent);
            throw new UnauthorizedException("Invalid email or password");
        }
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {


            String username = jwtService.extractUsername(request.refreshToken());
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (!jwtService.validateToken(request.refreshToken(), userDetails)
                    || !jwtService.passwordVersionMatches(request.refreshToken(), userDetails)
                    || tokenRevocationService.isRevoked(request.refreshToken())) {
                throw new UnauthorizedException("Invalid refresh token");
            }


            Map<String, Object> claims = tenantClaims(((AppUser) userDetails).getUser().getId());
            String newAccessToken = jwtService.generateAccessToken(userDetails, claims);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails, claims);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, "Token refreshed"));
        } catch (Exception e) {

            throw new UnauthorizedException("Invalid refresh token");
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            Long userId = resolveUserId(request.refreshToken());
            tokenRevocationService.revoke(request.refreshToken(), userId);
            auditLogService.record(userId, AuditEventType.LOGOUT,
                    "User logged out (refresh token revoked)",
                    clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }


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


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        loginAttemptService.checkAndRecordEmailSend(
                request.email(), clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        userService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Password reset email sent if account exists"));
    }


    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        userService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }


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


    public record LogoutRequest(
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


    private Map<String, Object> tenantClaims(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        storeService.findActiveStoreIdForOwner(userId)
                .ifPresent(storeId -> claims.put(JwtService.CLAIM_STORE_ID, storeId));
        return claims;
    }


    private Long resolveUserId(String token) {
        try {
            UserDetails userDetails = userService.loadUserByUsername(jwtService.extractUsername(token));
            return ((AppUser) userDetails).getUser().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
