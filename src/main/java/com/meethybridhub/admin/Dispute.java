package com.meethybridhub.admin;

import com.meethybridhub.store.TenantEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "disputes")
@EntityListeners(AuditingEntityListener.class)
public class Dispute extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "commission_id")
    private Long commissionId;

    @Column(name = "filed_by_id", nullable = false)
    private Long filedById;

    @Column(name = "assigned_to_id")
    private Long assignedToId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 30)
    private DisputeType disputeType;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputePriority priority = DisputePriority.NORMAL;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Dispute() {}

    public Dispute(Long storeId, Long orderId, Long commissionId, Long filedById,
                    DisputeType disputeType, String subject, String description,
                    DisputePriority priority) {
        setStoreId(storeId);
        this.orderId = orderId;
        this.commissionId = commissionId;
        this.filedById = filedById;
        this.disputeType = disputeType;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public Long getCommissionId() { return commissionId; }
    public Long getFiledById() { return filedById; }
    public Long getAssignedToId() { return assignedToId; }
    public DisputeType getDisputeType() { return disputeType; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public DisputeStatus getStatus() { return status; }
    public String getResolution() { return resolution; }
    public DisputePriority getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public void setPriority(DisputePriority priority) { this.priority = priority; }
}
