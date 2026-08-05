package com.meethybridhub.store;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A domain (or subdomain) mapped to a store — e.g. {@code divine-signature.meethybridhub.com}.
 *
 * This is the reference implementation of the {@link TenantEntity} pattern:
 * it carries a {@code store_id} column (via the mapped superclass) and is
 * queried exclusively through tenant-scoped repository methods so that one
 * store can never read another store's data.
 */
@Entity
@Table(name = "store_domains")
@EntityListeners(AuditingEntityListener.class)
public class StoreDomain extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(nullable = false)
    private boolean verified;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoreDomain() {}

    public StoreDomain(Long storeId, String domain, boolean primary, boolean verified) {
        setStoreId(storeId);
        this.domain = domain;
        this.primary = primary;
        this.verified = verified;
    }

    public Long getId() {
        return id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "StoreDomain{" +
                "id=" + id +
                ", storeId=" + getStoreId() +
                ", domain='" + domain + '\'' +
                ", primary=" + primary +
                ", verified=" + verified +
                '}';
    }
}
