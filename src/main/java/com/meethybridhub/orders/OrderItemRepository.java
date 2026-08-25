package com.meethybridhub.orders;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends TenantRepository<OrderItem, Long> {
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findByOrderId(String orderId);
}
