package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;


@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {


    long countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
            String email, LoginAttempt.Purpose purpose, boolean success, Instant after);


    long countByEmailAndPurposeAndCreatedAtAfter(String email, LoginAttempt.Purpose purpose, Instant after);


    long countByIpAddressAndPurposeAndCreatedAtAfter(
            String ipAddress, LoginAttempt.Purpose purpose, Instant after);


    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.email = :email AND a.success = false AND a.purpose = :purpose")
    int deleteFailedForEmail(@Param("email") String email, @Param("purpose") LoginAttempt.Purpose purpose);


    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :before")
    int deleteBefore(@Param("before") Instant before);
}
