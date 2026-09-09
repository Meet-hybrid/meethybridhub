package com.meethybridhub.store;

import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StoreSettingsRepository extends TenantRepository<StoreSettings, Long> {

    Optional<StoreSettings> findByStoreId(Long storeId);
}
