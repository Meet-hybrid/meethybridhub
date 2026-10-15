package com.meethybridhub.admin;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CommissionEntryRepository extends TenantRepository<CommissionEntry, Long> {

    @Query("SELECT e FROM CommissionEntry e WHERE e.storeId = :storeId ORDER BY e.createdAt DESC")
    List<CommissionEntry> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT COALESCE(SUM(e.commissionAmount), 0) FROM CommissionEntry e WHERE e.storeId = :storeId AND e.status = :status")
    BigDecimal sumCommissionByStoreAndStatus(Long storeId, CommissionStatus status);

    @Query("SELECT COUNT(e) FROM CommissionEntry e WHERE e.storeId = :storeId")
    long countByStoreId(Long storeId);
}
