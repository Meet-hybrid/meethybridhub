package com.meethybridhub.discovery;

import com.meethybridhub.identity.User;
import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "product_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "customer_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class ProductReview extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private short rating;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private boolean verified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductReview() {}

    public ProductReview(Long storeId, Long productId, User customer, short rating,
                          String title, String comment, boolean verified) {
        setStoreId(storeId);
        this.productId = productId;
        this.customer = customer;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.verified = verified;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getCustomerId() { return customer == null ? null : customer.getId(); }
    public short getRating() { return rating; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public boolean isVerified() { return verified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    void setId(Long id) { this.id = id; }
    public void setRating(short rating) { this.rating = rating; }
    public void setTitle(String title) { this.title = title; }
    public void setComment(String comment) { this.comment = comment; }
}
