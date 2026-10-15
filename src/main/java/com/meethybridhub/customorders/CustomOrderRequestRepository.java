package com.meethybridhub.customorders;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomOrderRequestRepository extends TenantRepository<CustomOrderRequest, Long> {

    @Query("SELECT r FROM CustomOrderRequest r WHERE r.customer.id = :customerId ORDER BY r.createdAt DESC")
    List<CustomOrderRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomOrderRequest> findByStatus(CustomOrderStatus status);

    @Query("SELECT r FROM CustomOrderRequest r WHERE r.storeId = :storeId ORDER BY r.createdAt DESC")
    List<CustomOrderRequest> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT r FROM CustomOrderRequest r WHERE r.storeId = :storeId AND r.status = :status ORDER BY r.createdAt DESC")
    List<CustomOrderRequest> findByStoreIdAndStatus(Long storeId, CustomOrderStatus status);
}
