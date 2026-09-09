package com.meethybridhub.billing;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "platform_charges")
@EntityListeners(AuditingEntityListener.class)
public class PlatformCharge {


    public enum Status {
        PENDING,
        COLLECTED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
    private String transactionRef;

    @Column(name = "transaction_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "charge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeAmount;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlatformCharge() {}

    public PlatformCharge(String transactionRef, BigDecimal transactionAmount,
                          BigDecimal chargeAmount, String currency) {
        this.transactionRef = transactionRef;
        this.transactionAmount = transactionAmount;
        this.chargeAmount = chargeAmount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public BigDecimal getChargeAmount() {
        return chargeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
