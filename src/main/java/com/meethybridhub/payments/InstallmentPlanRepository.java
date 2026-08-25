package com.meethybridhub.payments;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstallmentPlanRepository extends TenantRepository<InstallmentPlan, Long> {
    Optional<InstallmentPlan> findByStoreIdAndOrder_Id(Long storeId, Long orderId);
}
