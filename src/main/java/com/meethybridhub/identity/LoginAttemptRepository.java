package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for {@link LoginAttempt}. The windowed count queries back the
 * rate limiter; all filter by {@code purpose} so the login lockout and the
 * email-send limits never interfere, and each hits the {@code (email,
 * created_at)} / {@code (ip_address, created_at)} indexes from V2__identity.sql.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /** Failed LOGIN attempts for one email since {@code after} (account lockout). */
    long countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
            String email, LoginAttempt.Purpose purpose, boolean success, Instant after);

    /** EMAIL_SEND events targeting one email since {@code after} (email flood limit). */
    long countByEmailAndPurposeAndCreatedAtAfter(String email, LoginAttempt.Purpose purpose, Instant after);

    /** All events of one purpose from one IP since {@code after} (rate limiting). */
    long countByIpAddressAndPurposeAndCreatedAtAfter(
            String ipAddress, LoginAttempt.Purpose purpose, Instant after);

    /**
     * Clears the LOGIN failure history for one email — called on a successful
     * login so the lockout counter restarts from zero (the user just proved
     * they know the password). EMAIL_SEND rows are never touched.
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.email = :email AND a.success = false AND a.purpose = :purpose")
    int deleteFailedForEmail(@Param("email") String email, @Param("purpose") LoginAttempt.Purpose purpose);

    /**
     * Bulk-deletes records older than {@code before} — the daily maintenance
     * job keeps the table bounded (24h retention per V2 comment). Applies to
     * both purposes.
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :before")
    int deleteBefore(@Param("before") Instant before);
}
