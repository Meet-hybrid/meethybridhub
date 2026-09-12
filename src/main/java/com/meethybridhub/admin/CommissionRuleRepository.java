package com.meethybridhub.admin;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommissionRuleRepository extends TenantRepository<CommissionRule, Long> {

    @Query("SELECT r FROM CommissionRule r WHERE r.active = true AND (r.storeId = :storeId OR r.storeId IS NULL) ORDER BY r.minOrder ASC NULLS LAST")
    List<CommissionRule> findActiveForStore(Long storeId);
}
