package com.meethybridhub.admin;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dispute_messages")
@EntityListeners(AuditingEntityListener.class)
public class DisputeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DisputeMessage() {}

    public DisputeMessage(Dispute dispute, Long senderId, String message) {
        this.dispute = dispute;
        this.senderId = senderId;
        this.message = message;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public Dispute getDispute() { return dispute; }
    public Long getSenderId() { return senderId; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
