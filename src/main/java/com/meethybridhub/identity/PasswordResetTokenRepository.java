package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PasswordResetToken}.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserId(Long userId);

    /** Expired tokens are cleaned up by a scheduled job. */
    List<PasswordResetToken> findByExpiresAtBefore(Instant time);

    /** Invalidates outstanding (unused) reset tokens for a user before issuing a new one. */
    void deleteByUserIdAndUsedAtIsNull(Long userId);
}
