package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);


    Optional<User> findByEmailIgnoreCase(String email);


    boolean existsByEmail(String email);


    boolean existsByEmailIgnoreCase(String email);


    List<User> findByStatus(User.UserStatus status);


    List<User> findByCreatedAtAfter(Instant date);


    @Query("SELECT u FROM User u WHERE u.roles LIKE %:role%")
    List<User> findByRole(@Param("role") String role);


    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.emailVerified = true")
    List<User> findActiveUsers();


    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :date OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("date") Instant date);


    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("timestamp") Instant timestamp);
}
