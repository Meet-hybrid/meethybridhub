package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Expired tokens are cleaned up by a scheduled job. */
    List<EmailVerificationToken> findByExpiresAtBefore(Instant time);

    /** Invalidates outstanding (unused) tokens before issuing a fresh one on resend. */
    void deleteByUserIdAndUsedAtIsNull(Long userId);
}
