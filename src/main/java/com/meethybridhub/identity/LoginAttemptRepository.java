package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for {@link LoginAttempt}. The windowed count queries back the
 * rate limiter; both hit the {@code (email, created_at)} / {@code (ip_address,
 * created_at)} indexes from V2__identity.sql.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /** Failed attempts for one email since {@code after} (account lockout). */
    long countByEmailAndSuccessAndCreatedAtAfter(String email, boolean success, Instant after);

    /** All attempts from one IP since {@code after} (rate limiting). */
    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant after);

    /**
     * Clears the failure history for one email — called on a successful login
     * so the lockout counter restarts from zero (the user just proved they
     * know the password).
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.email = :email AND a.success = false")
    int deleteFailedForEmail(@Param("email") String email);

    /**
     * Bulk-deletes attempts older than {@code before} — the daily maintenance
     * job keeps the table bounded (24h retention per V2 comment).
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :before")
    int deleteBefore(@Param("before") Instant before);
}
