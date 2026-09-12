package com.meethybridhub.customorders;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuoteRepository extends TenantRepository<Quote, Long> {

    @Query("SELECT q FROM Quote q WHERE q.request.id = :requestId ORDER BY q.createdAt DESC")
    List<Quote> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    @Query("SELECT q FROM Quote q WHERE q.storeId = :storeId AND q.status = :status ORDER BY q.createdAt DESC")
    List<Quote> findByStoreIdAndStatus(Long storeId, QuoteStatus status);
}
