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
    }
}
