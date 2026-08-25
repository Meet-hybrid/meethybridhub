package com.meethybridhub.catalog;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends TenantRepository<Product, Long> {
    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);
    Page<Product> findAllByStoreIdAndActiveTrue(Long storeId, Pageable pageable);

    @Query("""
            select p from Product p
            where p.storeId = :storeId
              and (:activeOnly = false or p.active = true)
              and (:search is null or lower(p.name) like lower(concat('%', :search, '%')))
              and (:categoryId is null or p.category.id = :categoryId)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            """)
    Page<Product> search(@Param("storeId") Long storeId,
                         @Param("activeOnly") boolean activeOnly,
                         @Param("search") String search,
                         @Param("categoryId") Long categoryId,
                         @Param("minPrice") java.math.BigDecimal minPrice,
                         @Param("maxPrice") java.math.BigDecimal maxPrice,
                         Pageable pageable);
}
