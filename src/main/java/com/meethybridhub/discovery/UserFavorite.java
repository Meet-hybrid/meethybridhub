package com.meethybridhub.discovery;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "user_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private FavoriteEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFavorite() {}

    public UserFavorite(Long userId, FavoriteEntityType entityType, Long entityId) {
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public FavoriteEntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public Instant getCreatedAt() { return createdAt; }
}
