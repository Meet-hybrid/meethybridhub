package com.meethybridhub.discovery;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends TenantRepository<ProductReview, Long> {

    @Query("SELECT r FROM ProductReview r WHERE r.productId = :productId ORDER BY r.createdAt DESC")
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT r FROM ProductReview r WHERE r.productId = :productId AND r.customer.id = :customerId")
    Optional<ProductReview> findByProductIdAndCustomerId(Long productId, Long customerId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId")
    Double findAverageRatingByProductId(Long productId);
}
