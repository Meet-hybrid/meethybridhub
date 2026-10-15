package com.meethybridhub.customorders;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quotes")
@EntityListeners(AuditingEntityListener.class)
public class Quote extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomOrderRequest request;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuoteStatus status = QuoteStatus.PENDING;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quote() {}

    public Quote(Long storeId, CustomOrderRequest request, BigDecimal price,
                 String currency, Integer estimatedDays, String terms, Instant expiresAt) {
        setStoreId(storeId);
        this.request = request;
        this.price = price;
        this.currency = currency;
        this.estimatedDays = estimatedDays;
        this.terms = terms;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public CustomOrderRequest getRequest() { return request; }
    public Long getRequestId() { return request == null ? null : request.getId(); }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Integer getEstimatedDays() { return estimatedDays; }
    public String getTerms() { return terms; }
    public QuoteStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    void setId(Long id) { this.id = id; }
    public void setRequest(CustomOrderRequest request) { this.request = request; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setTerms(String terms) { this.terms = terms; }
    public void setStatus(QuoteStatus status) { this.status = status; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
