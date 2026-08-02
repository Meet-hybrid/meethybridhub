package com.meethybridhub.identity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Set;

/**
 * User entity representing a platform user (customer, store owner, or admin).
 *
 * This is a multi‑tenant‑aware entity, though tenant isolation (`store_id`)
 * will be added in Phase 3. For now, all users exist at the platform level.
 *
 * Roles are stored as a comma‑separated string for simplicity in Phase 2.
 * When RBAC complexity grows (Phase 8), we'll migrate to a proper role‑junction table.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String roles = "CUSTOMER";  // Default role

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    protected User() {}

    // Primary constructor for creating new users
    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    /**
     * Helper method to check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return Set.of(roles.split(",")).contains(role);
    }

    /**
     * Helper method to add a role if not already present.
     */
    public void addRole(String role) {
        if (!hasRole(role)) {
            this.roles = this.roles.isEmpty() ? role : this.roles + "," + role;
        }
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Business logic: whether the user is allowed to authenticate.
     */
    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE && emailVerified;
    }

    /**
     * Business logic: record a successful login.
     */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", status=" + status +
                ", emailVerified=" + emailVerified +
                '}';
    }

    /**
     * User account lifecycle states.
     */
    public enum UserStatus {
        PENDING,    // Created but email not verified
        ACTIVE,     // Verified and can log in
        SUSPENDED,  // Temporarily blocked
        DELETED     // Soft‑deleted (data retained for compliance)
    }
}