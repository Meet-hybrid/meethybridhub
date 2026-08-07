package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Server-side logout: revokes refresh tokens so {@code /refresh} rejects them
 * (see V10__revoked_tokens.sql and {@link RevokedToken}).
 *
 * Only the SHA-256 hash of a token is ever stored, never the token itself.
 * Access tokens (24h TTL) are intentionally NOT denylisted — they expire
 * naturally, keeping the stateless design free of per-request DB lookups.
 *
 * Revocation is idempotent: the unique {@code token_hash} constraint (plus the
 * exists-check below) means logging out twice records exactly one row.
 */
@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository, JwtService jwtService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Revoke a refresh token. Idempotent: a token already revoked (or an
     * unparseable token, which has nothing to revoke) is a no-op.
     *
     * @param token  the raw refresh token; never persisted, only hashed
     * @param userId the owning user, or null when unknown (best effort)
     */
    public void revoke(String token, Long userId) {
        if (token == null || token.isBlank()) {
            return;
        }
        final Instant expiresAt;
        try {
            expiresAt = jwtService.extractExpiration(token);
        } catch (Exception e) {
            // Can't parse the token — nothing meaningful to revoke. Log and move on.
            log.debug("Logout: could not parse refresh token, nothing to revoke: {}", e.getMessage());
            return;
        }

        String hash = sha256(token);
        if (revokedTokenRepository.existsByTokenHash(hash)) {
            log.debug("Logout: refresh token already revoked (idempotent logout)");
            return;
        }

        try {
            revokedTokenRepository.save(new RevokedToken(hash, userId, expiresAt));
            log.info("Refresh token revoked for user {}", userId);
        } catch (DataIntegrityViolationException e) {
            // Lost an exists-then-save race; the unique hash is the real guard.
            log.debug("Logout: token hash unique constraint caught a concurrent revoke");
        }
    }

    /** True when this refresh token has been revoked (logged out). */
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByTokenHash(sha256(token));
    }

    /** SHA-256 hex digest of the token — the only representation we persist. */
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
