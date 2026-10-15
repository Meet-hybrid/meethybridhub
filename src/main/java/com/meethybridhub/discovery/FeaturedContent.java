package com.meethybridhub.discovery;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "featured_content")
@EntityListeners(AuditingEntityListener.class)
public class FeaturedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private FeaturedContentType contentType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "link_url", columnDefinition = "TEXT")
    private String linkUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeaturedContent() {}

    public FeaturedContent(FeaturedContentType contentType, String title, String subtitle,
                            String imageUrl, String linkUrl, int sortOrder) {
        this.contentType = contentType;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public FeaturedContentType getContentType() { return contentType; }
    public Long getEntityId() { return entityId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }

    void setId(Long id) { this.id = id; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
}
