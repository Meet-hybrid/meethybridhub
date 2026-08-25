package com.meethybridhub.catalog;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends TenantRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByStoreIdAndProductId(Long storeId, Long productId);
    Optional<ProductVariant> findByStoreIdAndSku(Long storeId, String sku);
}
