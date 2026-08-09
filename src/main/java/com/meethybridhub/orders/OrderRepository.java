package com.meethybridhub.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStoreId(String storeId);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStoreIdAndCustomerId(String storeId, String customerId);
}
