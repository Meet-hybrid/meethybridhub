package com.meethybridhub.identity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A revoked refresh token (see V10__revoked_tokens.sql).
 *
 * Only the SHA-256 {@code hash} of the token is stored — never the token
 * itself. {@code expiresAt} mirrors the token's own expiry so the nightly
 * cleanup can purge rows once they would have expired anyway.
 *
 * One row per revoked token (unique hash), so logout is naturally idempotent.
 */
@Entity
@Table(name = "revoked_tokens")
@EntityListeners(AuditingEntityListener.class)
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType = "REFRESH";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    protected RevokedToken() {}

    public RevokedToken(String tokenHash, Long userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
