package com.meethybridhub.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token creation, validation, and parsing.
 *
 * Uses the modern jjwt API (0.13.x) which enforces secure defaults:
 *   - All signing keys must be ≥ 256‑bit (HS256 minimum).
 *   - Claims are validated automatically during parsing.
 *
 * Environment variable JWT_SECRET must be set (at least 32 random bytes).
 * In production, this should be a securely generated secret stored in a
 * secret manager, NOT in the codebase.
 */
@Service
public class JwtService {

    @Value("${jwt.secret:changemeinproductionatleast32bytessecrethere}")
    private String secret;

    @Value("${jwt.access-token.expiration-hours:24}")
    private int accessTokenExpirationHours;

    @Value("${jwt.refresh-token.expiration-days:30}")
    private int refreshTokenExpirationDays;

    /**
     * Extract username (email) from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token.
     */
    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    /**
     * Extract a specific claim from token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate access token for a user.
     *
     * @param userDetails User information
     * @return JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add custom claims as needed (roles, userId, etc.)
        return buildToken(claims, userDetails.getUsername(), accessTokenExpirationHours, ChronoUnit.HOURS);
    }

    /**
     * Generate refresh token for a user.
     * Refresh tokens have longer expiration and contain minimal claims.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails.getUsername(), refreshTokenExpirationDays, ChronoUnit.DAYS);
    }

    /**
     * Validate token against user details.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Check if token is expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    /**
     * Extract all claims from token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Build a JWT token with given parameters.
     */
    private String buildToken(Map<String, Object> claims, String subject,
                               long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiration = now.plus(amount, unit);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Get the signing key from the secret string.
     */
    private SecretKey getSigningKey() {
        // Ensure secret is at least 256 bits (32 chars) for HS256
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long. Current length: " + secret.length());
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Get remaining validity of token in minutes.
     * Useful for deciding whether to refresh.
     */
    public long getRemainingValidityMinutes(String token) {
        Instant expiration = extractExpiration(token);
        Instant now = Instant.now();
        if (expiration.isBefore(now)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(now, expiration);
    }
}