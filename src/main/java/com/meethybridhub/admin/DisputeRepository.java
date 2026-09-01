package com.meethybridhub.admin;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DisputeRepository extends TenantRepository<Dispute, Long> {

    @Query("SELECT d FROM Dispute d WHERE d.storeId = :storeId ORDER BY d.createdAt DESC")
    List<Dispute> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT d FROM Dispute d WHERE d.status = :status ORDER BY d.priority DESC, d.createdAt ASC")
    List<Dispute> findByStatus(DisputeStatus status);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = :status")
    long countByStatus(DisputeStatus status);
}
