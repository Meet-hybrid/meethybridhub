package com.meethybridhub.store;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link StoreSettings}. Extends {@link TenantRepository}, so
 * every read is automatically scoped to a {@code store_id}.
 */
@Repository
public interface StoreSettingsRepository extends TenantRepository<StoreSettings, Long> {

    Optional<StoreSettings> findByStoreId(Long storeId);
}
