package com.meethybridhub.store;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for every tenant-scoped (store-scoped) entity.
 *
 * Subclasses automatically get a non-null {@code store_id} column — the
 * shared-schema multi-tenancy strategy from the project docs. Combined with
 * {@link TenantRepository} (tenant-scoped queries) and {@link StoreFilter}
 * (resolves the current store into {@link TenantContext}), this is what keeps
 * each store's data isolated.
 *
 * Example for new entities (categories, orders, ...):
 * <pre>{@code
 * @Entity
 * @Table(name = "categories")
 * public class Category extends TenantEntity {
 *     @Id @GeneratedValue ... Long id;
 *     // ... own fields
 * }
 * }</pre>
 * and have the repository extend {@code TenantRepository<Category, Long>}.
 */
@MappedSuperclass
public abstract class TenantEntity {

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
