package com.meethybridhub.discovery;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreReviewRepository extends TenantRepository<StoreReview, Long> {

    @Query("SELECT r FROM StoreReview r WHERE r.storeId = :storeId ORDER BY r.createdAt DESC")
    List<StoreReview> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT r FROM StoreReview r WHERE r.storeId = :storeId AND r.customer.id = :customerId")
    Optional<StoreReview> findByStoreIdAndCustomerId(Long storeId, Long customerId);

    @Query("SELECT AVG(r.rating) FROM StoreReview r WHERE r.storeId = :storeId")
    Double findAverageRatingByStoreId(Long storeId);

    @Query("SELECT COUNT(r) FROM StoreReview r WHERE r.storeId = :storeId")
    long countByStoreId(Long storeId);
}
