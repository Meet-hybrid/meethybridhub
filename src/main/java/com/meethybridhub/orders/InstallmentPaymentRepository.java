package com.meethybridhub.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstallmentPaymentRepository extends JpaRepository<InstallmentPayment, String> {
    List<InstallmentPayment> findByInstallmentPlanIdOrderByDueDateAsc(String installmentPlanId);
}
