package com.meethybridhub.payments;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "installment_payments")
public class InstallmentPayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_plan_id", nullable = false)
    private InstallmentPlan plan;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstallmentPaymentStatus status = InstallmentPaymentStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected InstallmentPayment() {}

    public InstallmentPayment(Long storeId, int sequenceNumber, LocalDate dueDate, BigDecimal amount) {
        setStoreId(storeId);
        this.sequenceNumber = sequenceNumber;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Long getPlanId() { return plan.getId(); }
    public int getSequenceNumber() { return sequenceNumber; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getAmount() { return amount; }
    public InstallmentPaymentStatus getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPlan(InstallmentPlan plan) { this.plan = plan; }
}
