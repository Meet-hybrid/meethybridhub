package com.meethybridhub.payments;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallmentPaymentRepository extends TenantRepository<InstallmentPayment, Long> {
}
