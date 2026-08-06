package com.meethybridhub.identity;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pure unit tests for {@link JwtService} — no Spring context.
 *
 * JwtService uses {@code @Value} field injection, so a fresh instance has
 * null/zero fields until the test injects them via {@link ReflectionTestUtils}.
 * Tokens with custom claims/expirations are built directly with the jjwt API
 * (same signing key) so expiry and version scenarios are deterministic.
 */
class JwtServiceTest {

    /** ≥ 32 chars, as required by the HS256 minimum key size. */
    private static final String SECRET = "testsecretthatsatleast32characterslong!";
    private static final String OTHER_SECRET = "anothertestsecretthatisalsolongenough123";

    private static final int ACCESS_HOURS = 24;
    private static final int REFRESH_DAYS = 30;

    private JwtService service() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "accessTokenExpirationHours", ACCESS_HOURS);
        ReflectionTestUtils.setField(service, "refreshTokenExpirationDays", REFRESH_DAYS);
        return service;
    }

    private AppUser appUser(String email, int passwordVersion) {
        User user = new User(email, "hash", "Test User");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        for (int i = 0; i < passwordVersion; i++) {
            user.bumpPasswordVersion();
        }
        return new AppUser(user);
    }

    /** Sign a token with the test secret and arbitrary claims/expiry. */
    private String signedToken(String subject, Instant expiration, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), Jwts.SIG.HS256)
                .compact();
    }

    // ------------------------------------------------------------------
    // Token generation / round-trip
    // ------------------------------------------------------------------

    @Test
    void generatedAccessTokenRoundTripsSubjectAndExpiration() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);

        String token = service.generateAccessToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(service.extractExpiration(token)).isBetween(
                Instant.now().plus(ACCESS_HOURS - 1, ChronoUnit.HOURS),
                Instant.now().plus(ACCESS_HOURS, ChronoUnit.HOURS).plusSeconds(1));
        assertThat(service.getRemainingValidityMinutes(token)).isBetween(1430L, 1440L);
    }

    @Test
    void generatedRefreshTokenIsValidAndLongLived() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);

        String token = service.generateRefreshToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(service.getRemainingValidityMinutes(token)).isBetween(43180L, 43200L);
        assertThat(service.validateToken(token, user)).isTrue();
    }

    @Test
    void extraClaimsAreEmbeddedAndReadable() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);

        String token = service.generateAccessToken(user, Map.of(JwtService.CLAIM_STORE_ID, 42));

        Integer storeId = service.extractClaim(token, claims -> claims.get(JwtService.CLAIM_STORE_ID, Integer.class));
        assertThat(storeId).isEqualTo(42);
    }

    @Test
    void shortSecretFailsFast() {
        JwtService service = service();
        ReflectionTestUtils.setField(service, "secret", "short");

        assertThatThrownBy(() -> service.generateAccessToken(appUser("a@b.com", 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    // ------------------------------------------------------------------
    // validateToken
    // ------------------------------------------------------------------

    @Test
    void validateTokenAcceptsValidTokenForMatchingUser() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);

        assertThat(service.validateToken(service.generateAccessToken(user), user)).isTrue();
    }

    @Test
    void validateTokenRejectsTokenForDifferentUser() {
        JwtService service = service();
        AppUser alice = appUser("alice@example.com", 0);
        AppUser bob = appUser("bob@example.com", 0);

        String aliceToken = service.generateAccessToken(alice);

        assertThat(service.validateToken(aliceToken, bob)).isFalse();
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        JwtService service = service();

        String expired = signedToken("alice@example.com",
                Instant.now().minus(1, ChronoUnit.HOURS), Map.of());

        assertThat(service.validateToken(expired, appUser("alice@example.com", 0))).isFalse();
    }

    @Test
    void validateTokenRejectsMalformedToken() {
        JwtService service = service();

        assertThat(service.validateToken("not.a.jwt", appUser("alice@example.com", 0))).isFalse();
    }

    @Test
    void validateTokenRejectsTokenSignedWithDifferentKey() {
        JwtService service = service();

        // A cleanly-signed token under a *different* secret must fail signature
        // verification (the realistic cross-secret scenario).
        String wrongKeyToken = Jwts.builder()
                .subject("alice@example.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(OTHER_SECRET.getBytes()), Jwts.SIG.HS256)
                .compact();

        assertThat(service.validateToken(wrongKeyToken, appUser("alice@example.com", 0))).isFalse();
    }

    // ------------------------------------------------------------------
    // passwordVersionMatches
    // ------------------------------------------------------------------

    @Test
    void passwordVersionMatchesAcceptsTokenForCurrentVersion() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 2);

        assertThat(service.passwordVersionMatches(service.generateAccessToken(user), user)).isTrue();
    }

    @Test
    void passwordVersionMatchesRejectsTokenIssuedBeforePasswordChange() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);
        String oldToken = service.generateAccessToken(user);

        user.getUser().bumpPasswordVersion(); // password changed after issuance

        assertThat(service.passwordVersionMatches(oldToken, user)).isFalse();
    }

    @Test
    void passwordVersionMatchesRejectsTokenWithoutClaim() {
        JwtService service = service();

        // A token signed correctly but without the pwdv claim (e.g. pre-feature
        // tokens) must not be trusted.
        String noClaimToken = signedToken("alice@example.com",
                Instant.now().plus(1, ChronoUnit.HOURS), Map.of());

        assertThat(service.passwordVersionMatches(noClaimToken, appUser("alice@example.com", 0))).isFalse();
    }

    @Test
    void passwordVersionMatchesAllowsNonAppUserPrincipal() {
        JwtService service = service();
        AppUser user = appUser("alice@example.com", 0);

        UserDetails foreignPrincipal = mock(UserDetails.class);

        assertThat(service.passwordVersionMatches(service.generateAccessToken(user), foreignPrincipal)).isTrue();
    }

    // ------------------------------------------------------------------
    // getRemainingValidityMinutes
    // ------------------------------------------------------------------

    @Test
    void getRemainingValidityMinutesThrowsForExpiredToken() {
        JwtService service = service();

        // jjwt rejects expired tokens at parse time, so callers of a genuinely
        // expired token see ExpiredJwtException rather than 0.
        String expired = signedToken("alice@example.com",
                Instant.now().minus(1, ChronoUnit.MINUTES), Map.of());

        assertThatThrownBy(() -> service.getRemainingValidityMinutes(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void getRemainingValidityMinutesReturnsZeroWhenExpirationPassed() {
        // The isBefore(now) guard is unreachable through the real parser (jjwt
        // throws for expired tokens first), so cover it with a stub that
        // reports a past expiration directly.
        JwtService service = new JwtService() {
            @Override
            public Instant extractExpiration(String token) {
                return Instant.now().minus(1, ChronoUnit.MINUTES);
            }
        };
        // The stub bypasses parsing, but keep the secret injected anyway so the
        // test survives a refactor of getRemainingValidityMinutes.
        ReflectionTestUtils.setField(service, "secret", SECRET);

        assertThat(service.getRemainingValidityMinutes("anything")).isZero();
    }

    @Test
    void getRemainingValidityMinutesCountsDownForNearExpiryToken() {
        JwtService service = service();

        String nearlyExpired = signedToken("alice@example.com",
                Instant.now().plus(2, ChronoUnit.MINUTES), Map.of());

        assertThat(service.getRemainingValidityMinutes(nearlyExpired)).isBetween(1L, 2L);
    }
}
