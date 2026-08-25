package com.meethybridhub.payments;

import com.meethybridhub.orders.Order;
import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "installment_plans")
@EntityListeners(AuditingEntityListener.class)
public class InstallmentPlan extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstallmentPlanStatus status = InstallmentPlanStatus.ACTIVE;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<InstallmentPayment> payments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstallmentPlan() {}

    public InstallmentPlan(Long storeId, Order order, int installmentCount, BigDecimal installmentAmount) {
        setStoreId(storeId);
        this.order = order;
        this.totalAmount = order.getTotalAmount();
        this.installmentCount = installmentCount;
        this.installmentAmount = installmentAmount;
    }

    public void addPayment(InstallmentPayment payment) {
        payments.add(payment);
        payment.setPlan(this);
    }

    public Long getId() { return id; }
    public Long getOrderId() { return order.getId(); }
    public String getOrderNumber() { return order.getOrderNumber(); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getInstallmentCount() { return installmentCount; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public InstallmentPlanStatus getStatus() { return status; }
    public List<InstallmentPayment> getPayments() { return payments; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
