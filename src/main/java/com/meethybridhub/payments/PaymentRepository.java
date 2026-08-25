package com.meethybridhub.payments;

import com.meethybridhub.store.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends TenantRepository<Payment, Long> {
    Optional<Payment> findByStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);
    Optional<Payment> findByStoreIdAndTransactionId(Long storeId, String transactionId);
}
