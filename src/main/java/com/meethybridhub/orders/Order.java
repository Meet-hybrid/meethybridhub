package com.meethybridhub.orders;

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
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "customer_email", nullable = false, length = 320)
    private String customerEmail;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {}

    public Order(Long storeId, User customer, String orderNumber, String customerEmail,
                 String shippingAddress, String billingAddress, String notes) {
        setStoreId(storeId);
        this.customer = customer;
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.notes = notes;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customer == null ? null : customer.getId(); }
    public String getOrderNumber() { return orderNumber; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getCustomerEmail() { return customerEmail; }
    public String getShippingAddress() { return shippingAddress; }
    public String getBillingAddress() { return billingAddress; }
    public String getNotes() { return notes; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
