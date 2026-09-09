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


@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository, JwtService jwtService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
    }


    public void revoke(String token, Long userId) {
        if (token == null || token.isBlank()) {
            return;
        }
        final Instant expiresAt;
        try {
            expiresAt = jwtService.extractExpiration(token);
        } catch (Exception e) {

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

            log.debug("Logout: token hash unique constraint caught a concurrent revoke");
        }
    }


    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByTokenHash(sha256(token));
    }


    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
