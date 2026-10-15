package com.meethybridhub.customorders;

import com.meethybridhub.identity.User;
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
@Table(name = "custom_order_requests")
@EntityListeners(AuditingEntityListener.class)
public class CustomOrderRequest extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CustomOrderStatus status = CustomOrderStatus.OPEN;

    @Column(name = "budget_min", precision = 19, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 19, scale = 2)
    private BigDecimal budgetMax;

    private Instant deadline;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quote> quotes = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomOrderConversation> messages = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomOrderRequest() {}

    public CustomOrderRequest(Long storeId, User customer, String title, String description,
                              BigDecimal budgetMin, BigDecimal budgetMax, Instant deadline) {
        setStoreId(storeId);
        this.customer = customer;
        this.title = title;
        this.description = description;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.deadline = deadline;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customer == null ? null : customer.getId(); }
    public User getCustomer() { return customer; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CustomOrderStatus getStatus() { return status; }
    public BigDecimal getBudgetMin() { return budgetMin; }
    public BigDecimal getBudgetMax() { return budgetMax; }
    public Instant getDeadline() { return deadline; }
    public List<Quote> getQuotes() { return quotes; }
    public List<CustomOrderConversation> getMessages() { return messages; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(CustomOrderStatus status) { this.status = status; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }

    public void addQuote(Quote quote) {
        quotes.add(quote);
        quote.setRequest(this);
    }

    public void addMessage(CustomOrderConversation message) {
        messages.add(message);
        message.setRequest(this);
    }
}
