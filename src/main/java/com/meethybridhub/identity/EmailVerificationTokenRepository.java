package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link EmailVerificationToken}.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserId(Long userId);

    /**
     * Bulk-deletes every token that expired before {@code now}.
     * Called by the daily cleanup job (TokenCleanupService); returns the
     * number of deleted rows. The expires_at index makes this cheap.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    /** Invalidates outstanding (unused) tokens before issuing a fresh one on resend. */
    void deleteByUserIdAndUsedAtIsNull(Long userId);
}
