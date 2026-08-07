package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for {@link RevokedToken} rows (the logout denylist).
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    /** Purge revoked tokens that would have expired by {@code cutoff} (nightly cleanup). */
    long deleteByExpiresAtBefore(Instant cutoff);
}
