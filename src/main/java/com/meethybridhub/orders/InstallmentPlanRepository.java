package com.meethybridhub.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, String> {
    List<InstallmentPlan> findByOrderId(String orderId);
}
