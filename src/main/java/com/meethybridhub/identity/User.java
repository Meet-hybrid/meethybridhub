package com.meethybridhub.identity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Set;


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
    private String roles = "CUSTOMER";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;


    @Column(name = "password_version", nullable = false)
    private int passwordVersion = 0;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    protected User() {}


    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }


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


    public boolean hasRole(String role) {
        return Set.of(roles.split(",")).contains(role);
    }


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

    public int getPasswordVersion() {
        return passwordVersion;
    }


    public void bumpPasswordVersion() {
        this.passwordVersion++;
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


    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE && emailVerified;
    }


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


    public enum UserStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}
