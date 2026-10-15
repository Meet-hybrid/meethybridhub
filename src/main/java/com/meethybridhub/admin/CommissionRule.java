package com.meethybridhub.admin;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commission_rules")
@EntityListeners(AuditingEntityListener.class)
public class CommissionRule extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private CommissionRuleType ruleType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(name = "min_order", precision = 19, scale = 2)
    private BigDecimal minOrder;

    @Column(name = "max_order", precision = 19, scale = 2)
    private BigDecimal maxOrder;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CommissionRule() {}

    public CommissionRule(Long storeId, CommissionRuleType ruleType, BigDecimal rate,
                           String currency, BigDecimal minOrder, BigDecimal maxOrder) {
        setStoreId(storeId);
        this.ruleType = ruleType;
        this.rate = rate;
        this.currency = currency;
        this.minOrder = minOrder;
        this.maxOrder = maxOrder;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public CommissionRuleType getRuleType() { return ruleType; }
    public BigDecimal getRate() { return rate; }
    public String getCurrency() { return currency; }
    public BigDecimal getMinOrder() { return minOrder; }
    public BigDecimal getMaxOrder() { return maxOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setRate(BigDecimal rate) { this.rate = rate; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal calculate(BigDecimal orderAmount) {
        return switch (ruleType) {
            case FLAT_FEE -> rate;
            case PERCENTAGE -> orderAmount.multiply(rate).divide(new BigDecimal("100"));
            case TIERED -> {
                if (minOrder != null && orderAmount.compareTo(minOrder) < 0) yield BigDecimal.ZERO;
                if (maxOrder != null && orderAmount.compareTo(maxOrder) > 0) yield BigDecimal.ZERO;
                yield orderAmount.multiply(rate).divide(new BigDecimal("100"));
            }
        };
    }
}
