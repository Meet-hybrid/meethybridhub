package com.meethybridhub.admin;

import com.meethybridhub.store.TenantRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SalesSnapshotRepository extends TenantRepository<SalesSnapshot, Long> {

    @Query("SELECT s FROM SalesSnapshot s WHERE s.storeId = :storeId AND s.snapshotDate BETWEEN :from AND :to ORDER BY s.snapshotDate ASC")
    List<SalesSnapshot> findByStoreIdAndDateRange(Long storeId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(s.revenue), 0) FROM SalesSnapshot s WHERE s.storeId = :storeId AND s.snapshotDate BETWEEN :from AND :to")
    BigDecimal sumRevenueByStoreAndDateRange(Long storeId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(s.orderCount), 0) FROM SalesSnapshot s WHERE s.storeId = :storeId AND s.snapshotDate BETWEEN :from AND :to")
    long sumOrdersByStoreAndDateRange(Long storeId, LocalDate from, LocalDate to);
}
