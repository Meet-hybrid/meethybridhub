package com.meethybridhub.catalog;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends TenantRepository<Inventory, Long> {
    Optional<Inventory> findByStoreIdAndVariantId(Long storeId, Long variantId);
}
