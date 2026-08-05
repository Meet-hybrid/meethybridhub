package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entities.
 *
 * Spring Data automatically provides implementations for standard CRUD operations.
 * Custom queries are defined using method naming conventions or @Query annotations.
 *
 * Repository interfaces are scanned by Spring Boot's @EnableJpaRepositories
 * (enabled by default via spring-boot-starter-data-jpa).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (case-insensitive).
     * Returns Optional to handle null cases gracefully.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email ignoring case.
     * Useful for registration validation.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email exists in database.
     * Returns true if at least one user with the email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Check if email exists ignoring case.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find users by status.
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * Find users created after a specific date.
     */
    List<User> findByCreatedAtAfter(Instant date);

    /**
     * Find users with a specific role.
     * Uses custom query because roles are stored as comma-separated string.
     */
    @Query("SELECT u FROM User u WHERE u.roles LIKE %:role%")
    List<User> findByRole(@Param("role") String role);

    /**
     * Find active users (status = ACTIVE and email verified).
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.emailVerified = true")
    List<User> findActiveUsers();

    /**
     * Find users who haven't logged in since a specific date.
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :date OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("date") Instant date);

    /**
     * Update user's last login timestamp.
     * Uses @Modifying query for update operations.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("timestamp") Instant timestamp);
}