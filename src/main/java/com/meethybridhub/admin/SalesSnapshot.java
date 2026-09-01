package com.meethybridhub.admin;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sales_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"store_id", "snapshot_date"})
})
@EntityListeners(AuditingEntityListener.class)
public class SalesSnapshot extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "order_count", nullable = false)
    private int orderCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "new_customers", nullable = false)
    private int newCustomers = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SalesSnapshot() {}

    public SalesSnapshot(Long storeId, LocalDate snapshotDate) {
        setStoreId(storeId);
        this.snapshotDate = snapshotDate;
    }

    public Long getId() { return id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public int getOrderCount() { return orderCount; }
    public BigDecimal getRevenue() { return revenue; }
    public BigDecimal getCommission() { return commission; }
    public int getNewCustomers() { return newCustomers; }
    public Instant getCreatedAt() { return createdAt; }

    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
    public void setNewCustomers(int newCustomers) { this.newCustomers = newCustomers; }
}
