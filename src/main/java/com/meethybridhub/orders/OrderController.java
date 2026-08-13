package com.meethybridhub.orders;

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
