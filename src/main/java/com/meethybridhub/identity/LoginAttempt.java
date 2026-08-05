package com.meethybridhub.identity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A single login attempt or email-send event, tracked for rate limiting and
 * account lockout.
 *
 * Maps to the {@code login_attempts} table created in V2__identity.sql (indexed
 * on {@code (email, created_at)} and {@code (ip_address, created_at)}; the
 * {@code purpose} column was added by V5). Records older than 24 hours are
 * purged by the daily maintenance job.
 */
@Entity
@Table(name = "login_attempts")
@EntityListeners(AuditingEntityListener.class)
public class LoginAttempt {

    /**
     * What kind of event this row records. Counters always filter by purpose
     * so login lockouts and email-send limits never interfere.
     */
    public enum Purpose {
        LOGIN, EMAIL_SEND
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(nullable = false)
    private String email;

    // Stored as VARCHAR(45) (IPv6 max length) — see V5 migration. H2 has no
    // INET type, so a plain String keeps prod (validate) and tests in sync.
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failed_reason")
    private String failedReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LoginAttempt() {}

    /** Login-attempt row (purpose = LOGIN). */
    public LoginAttempt(String email, String ipAddress, String userAgent, boolean success, String failedReason) {
        this(Purpose.LOGIN, email, ipAddress, userAgent, success, failedReason);
    }

    public LoginAttempt(Purpose purpose, String email, String ipAddress, String userAgent, boolean success, String failedReason) {
        this.purpose = purpose;
        this.email = email;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failedReason = failedReason;
    }

    public Long getId() {
        return id;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public String getEmail() {
        return email;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
