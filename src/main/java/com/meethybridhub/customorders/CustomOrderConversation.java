package com.meethybridhub.customorders;

import com.meethybridhub.identity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "custom_order_conversations")
@EntityListeners(AuditingEntityListener.class)
public class CustomOrderConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomOrderRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomOrderConversation() {}

    public CustomOrderConversation(CustomOrderRequest request, User sender, String message) {
        this.request = request;
        this.sender = sender;
        this.message = message;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public CustomOrderRequest getRequest() { return request; }
    public void setRequest(CustomOrderRequest request) { this.request = request; }
    public Long getSenderId() { return sender == null ? null : sender.getId(); }
    public User getSender() { return sender; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
