package com.meethybridhub.orders;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends TenantRepository<OrderItem, Long> {
}
