package com.meethybridhub.admin;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commission_entries")
@EntityListeners(AuditingEntityListener.class)
public class CommissionEntry extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "commission_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommissionEntry() {}

    public CommissionEntry(Long storeId, Long orderId, Long ruleId,
                            BigDecimal orderAmount, BigDecimal commissionAmount, String currency) {
        setStoreId(storeId);
        this.orderId = orderId;
        this.ruleId = ruleId;
        this.orderAmount = orderAmount;
        this.commissionAmount = commissionAmount;
        this.currency = currency;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public Long getRuleId() { return ruleId; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public String getCurrency() { return currency; }
    public CommissionStatus getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(CommissionStatus status) { this.status = status; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
