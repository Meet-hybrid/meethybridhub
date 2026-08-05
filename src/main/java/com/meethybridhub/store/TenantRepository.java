package com.meethybridhub.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Base repository for tenant-scoped entities.
 *
 * Extending this instead of plain {@link JpaRepository} gives every
 * store-scoped repository read methods that are ALWAYS filtered by
 * {@code store_id} — the single enforcement point for row-level isolation.
 * Callers typically pass {@code TenantContext.requireStoreId()}.
 *
 * @param <T>  a {@link TenantEntity} subclass
 * @param <ID> the entity's id type
 */
@NoRepositoryBean
public interface TenantRepository<T extends TenantEntity, ID> extends JpaRepository<T, ID> {

    List<T> findAllByStoreId(Long storeId);

    Optional<T> findByIdAndStoreId(ID id, Long storeId);

    boolean existsByIdAndStoreId(ID id, Long storeId);

    long countByStoreId(Long storeId);
}
