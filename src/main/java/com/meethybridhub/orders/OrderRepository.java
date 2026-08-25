package com.meethybridhub.orders;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends TenantRepository<Order, Long> {
    Optional<Order> findByStoreIdAndOrderNumber(Long storeId, String orderNumber);
    Page<Order> findAllByStoreId(Long storeId, Pageable pageable);
    Page<Order> findAllByStoreIdAndCustomerId(Long storeId, Long customerId, Pageable pageable);
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStoreId(String storeId);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStoreIdAndCustomerId(String storeId, String customerId);
}
