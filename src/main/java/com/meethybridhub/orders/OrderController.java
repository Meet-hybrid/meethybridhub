package com.meethybridhub.orders;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    /** Guest checkout is allowed; an authenticated caller is attached as customer. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request) {
        User customer = userDetails == null ? null : userService.getUserByEmail(userDetails.getUsername());
        Order order = orderService.create(TenantContext.requireStoreId(), customer, request.customerEmail(),
                request.shippingAddress(), request.billingAddress(), request.notes(),
                request.items().stream().map(item -> new OrderService.LineRequest(item.variantId(), item.quantity())).toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(orderService.list(TenantContext.requireStoreId(), user, pageable).map(OrderResponse::from));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> get(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long orderId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(OrderResponse.from(orderService.get(TenantContext.requireStoreId(), orderId, user)));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancel(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long orderId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(OrderResponse.from(orderService.cancel(TenantContext.requireStoreId(), orderId, user)));
    }

    public record CreateOrderRequest(
            @NotBlank @Email String customerEmail,
            @NotBlank @Size(max = 3000) String shippingAddress,
            @Size(max = 3000) String billingAddress,
            @Size(max = 2000) String notes,
            @NotEmpty List<@Valid OrderLineRequest> items) {}

    public record OrderLineRequest(
            @NotNull Long variantId,
            @Min(1) int quantity) {}

    public record OrderResponse(Long id, Long storeId, Long customerId, String orderNumber,
                                OrderStatus status, BigDecimal totalAmount, String customerEmail,
                                String shippingAddress, String billingAddress, String notes,
                                List<OrderItemResponse> items, Instant createdAt) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getStoreId(), order.getCustomerId(), order.getOrderNumber(),
                    order.getStatus(), order.getTotalAmount(), order.getCustomerEmail(), order.getShippingAddress(),
                    order.getBillingAddress(), order.getNotes(), order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getCreatedAt());
        }
    }

    public record OrderItemResponse(Long id, Long variantId, String sku, String productName,
                                    int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getId(), item.getProductVariant().getId(), item.getSku(),
                    item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
        }
import com.meethybridhub.orders.dto.OrderDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Orders
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(req));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders(
        @RequestParam(required = false) String store_id,
        @RequestParam(required = false) String customer_id
    ) {
        return ResponseEntity.ok(orderService.getOrders(store_id, customer_id));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        Order order = orderService.getOrderById(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String id) {
        Order order = orderService.cancelOrder(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @GetMapping("/orders/{id}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }

    // Payments
    @PostMapping("/payments")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createPayment(req));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String id) {
        Payment payment = orderService.getPaymentById(id);
        return payment != null ? ResponseEntity.ok(payment) : ResponseEntity.notFound().build();
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<Payment> paymentWebhook(@Valid @RequestBody PaymentWebhookRequest req) {
        Payment payment = orderService.handleWebhook(req);
        return payment != null ? ResponseEntity.ok(payment) : ResponseEntity.notFound().build();
    }

    // Installments
    @GetMapping("/installment-plans")
    public ResponseEntity<List<InstallmentPlan>> getInstallmentPlans(@RequestParam(required = false) String order_id) {
        return ResponseEntity.ok(orderService.getInstallmentPlans(order_id));
    }

    @PostMapping("/installment-plans")
    public ResponseEntity<InstallmentPlan> createInstallmentPlan(@Valid @RequestBody CreateInstallmentPlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createInstallmentPlan(req));
    }

    @GetMapping("/installment-plans/{id}")
    public ResponseEntity<InstallmentPlan> getInstallmentPlanById(@PathVariable String id) {
        InstallmentPlan plan = orderService.getInstallmentPlanById(id);
        return plan != null ? ResponseEntity.ok(plan) : ResponseEntity.notFound().build();
    }

    @PostMapping("/installment-payments")
    public ResponseEntity<InstallmentPayment> processInstallmentPayment(@Valid @RequestBody ProcessInstallmentPaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.processInstallmentPayment(req));
    }

    // Shipping & Tax
    @GetMapping("/shipping/calculate")
    public ResponseEntity<List<ShippingMethod>> calculateShipping(
        @RequestParam String store_id,
        @RequestParam String country,
        @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(orderService.calculateShipping(store_id, country, state));
    }

    @GetMapping("/shipping/methods")
    public ResponseEntity<List<ShippingMethod>> getShippingMethods(@RequestParam String store_id) {
        return ResponseEntity.ok(orderService.getShippingMethods(store_id));
    }

    @GetMapping("/tax/calculate")
    public ResponseEntity<TaxCalculationResult> calculateTax(
        @RequestParam String store_id,
        @RequestParam String country,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(orderService.calculateTax(store_id, country, amount, state));
    }

    @GetMapping("/tax/rates")
    public ResponseEntity<List<TaxRate>> getTaxRates(@RequestParam String store_id) {
        return ResponseEntity.ok(orderService.getTaxRates(store_id));
    }
}
