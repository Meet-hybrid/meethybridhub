package com.meethybridhub.store;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Storefront branding and settings for a store (1:1 — see V9__store_settings.sql).
 *
 * Store-scoped via {@link TenantEntity}: carries a {@code store_id} column and
 * is only reachable through the owner's own tenant context, so one store can
 * never read or change another store's branding.
 *
 * A settings row is created lazily on first access with sensible defaults, so
 * a freshly created store always has valid branding to render.
 */
@Entity
@Table(name = "store_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_store_settings_store", columnNames = "store_id"))
@EntityListeners(AuditingEntityListener.class)
public class StoreSettings extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor = "#111111";

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor = "#0d9488";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreTheme theme = StoreTheme.LIGHT;

    @Column(length = 200)
    private String tagline;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoreSettings() {}

    public StoreSettings(Long storeId) {
        setStoreId(storeId);
    }

    public Long getId() {
        return id;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public StoreTheme getTheme() {
        return theme;
    }

    public void setTheme(StoreTheme theme) {
        this.theme = theme;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
