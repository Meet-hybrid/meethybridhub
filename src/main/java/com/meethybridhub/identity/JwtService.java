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

    /**
     * Claim carrying the user's password version. Tokens whose claim no longer
     * matches the user's current version are rejected, so a password change
     * invalidates every previously issued token.
     */
    private static final String CLAIM_PASSWORD_VERSION = "pwdv";

    /**
     * Claim carrying the store ID the user owns (if any) at token issuance.
     * StoreFilter uses it to resolve the tenant for store-owner dashboards
     * without an explicit X-Store-Id header or subdomain.
     */
    public static final String CLAIM_STORE_ID = "storeId";

    @Value("${jwt.secret:changemeinproductionatleast32bytessecrethere}")
    private String secret;

    @Value("${jwt.access-token.expiration-hours:24}")
    private int accessTokenExpirationHours;

    @Value("${jwt.refresh-token.expiration-days:30}")
    private int refreshTokenExpirationDays;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, Map.of());
    }

    public String generateAccessToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = baseClaims(userDetails);
        claims.putAll(extraClaims);
        return buildToken(claims, userDetails.getUsername(),
                accessTokenExpirationHours, ChronoUnit.HOURS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails, Map.of());
    }

    public String generateRefreshToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = baseClaims(userDetails);
        claims.put("type", "refresh");
        claims.putAll(extraClaims);
        return buildToken(claims, userDetails.getUsername(),
                refreshTokenExpirationDays, ChronoUnit.DAYS);
    }

    /**
     * Claims common to every token, currently the password version so that
     * tokens issued before a password change are rejected.
     */
    private Map<String, Object> baseClaims(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof AppUser appUser) {
            claims.put(CLAIM_PASSWORD_VERSION, appUser.getPasswordVersion());
        }
        return claims;
    }


    public boolean passwordVersionMatches(String token, UserDetails userDetails) {        if (userDetails instanceof AppUser appUser) {
            Integer tokenVersion = extractClaim(token,
                    claims -> claims.get(CLAIM_PASSWORD_VERSION, Integer.class));
            return tokenVersion != null && tokenVersion.equals(appUser.getPasswordVersion());
        }
        return true; // non-AppUser principal (shouldn't happen) — don't block
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

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

    private SecretKey getSigningKey() {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long. Current length: " + secret.length());
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public long getRemainingValidityMinutes(String token) {
        Instant expiration = extractExpiration(token);
        Instant now = Instant.now();
        if (expiration.isBefore(now)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(now, expiration);
    }
}