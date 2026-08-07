package com.meethybridhub.identity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * One entry in the immutable security audit trail ({@code audit_log} table,
 * V2__identity.sql).
 *
 * Append-only by convention: nothing in the codebase updates or deletes these
 * rows. The {@code user_id} is nullable so unauthenticated events (failed
 * logins) and events for users deleted later keep their trail.
 *
 * NOTE: the table's {@code metadata JSONB} column is deliberately NOT mapped —
 * H2 (tests build the schema from entities) cannot create JSONB from JPA, and
 * Hibernate's {@code validate} ignores unmapped columns. Revisit with a JSON
 * column type / Testcontainers when the first feature needs structured
 * payloads.
 */
@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platform user who triggered the event; null for unauthenticated events. */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuditEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Stored as VARCHAR(45) (IPv6 max length) — see V7 migration. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(Long userId, AuditEventType eventType, String description,
                    String ipAddress, String userAgent) {
        this.userId = userId;
        this.eventType = eventType;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
