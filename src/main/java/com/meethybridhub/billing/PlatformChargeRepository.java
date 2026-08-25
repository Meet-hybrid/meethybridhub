package com.meethybridhub.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository for {@link PlatformCharge} rows.
 */
@Repository
public interface PlatformChargeRepository extends JpaRepository<PlatformCharge, Long> {

    /** Idempotency check: has this transaction already been charged? */
    boolean existsByTransactionRef(String transactionRef);

    /** All charges for the given transaction refs (sweep filter). */
    List<PlatformCharge> findByTransactionRefIn(Collection<String> transactionRefs);

    long countByStatus(PlatformCharge.Status status);
}
