package com.meethybridhub.catalog;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends TenantRepository<Category, Long> {
    List<Category> findAllByStoreId(Long storeId);
    Optional<Category> findByStoreIdAndName(Long storeId, String name);
}
