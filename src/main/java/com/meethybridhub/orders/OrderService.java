package com.meethybridhub.orders;

import com.meethybridhub.catalog.Inventory;
import com.meethybridhub.catalog.InventoryRepository;
import com.meethybridhub.catalog.ProductVariant;
import com.meethybridhub.catalog.ProductVariantRepository;
import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductVariantRepository variantRepository,
                        InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Order create(Long storeId, User customer, String customerEmail, String shippingAddress,
                        String billingAddress, String notes, java.util.List<LineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("An order must contain at least one item");
        }
        Order order = new Order(storeId, customer, generateOrderNumber(), customerEmail.trim(),
                shippingAddress.trim(), blankToNull(billingAddress), blankToNull(notes));
        BigDecimal total = BigDecimal.ZERO;
        for (LineRequest line : lines) {
            if (line.quantity() <= 0) {
                throw new BadRequestException("Order item quantity must be positive");
            }
            ProductVariant variant = variantRepository.findByIdAndStoreId(line.variantId(), storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + line.variantId()));
            if (!variant.isActive() || !variant.getProduct().isActive()) {
                throw new BadRequestException("Product variant is not available: " + line.variantId());
            }
            Inventory inventory = inventoryRepository.findByStoreIdAndVariantId(storeId, variant.getId())
                    .orElseThrow(() -> new BadRequestException("No inventory configured for variant: " + line.variantId()));
            if (inventory.getAvailableQuantity() < line.quantity()) {
                throw new BadRequestException("Insufficient inventory for SKU: " + variant.getSku());
            }
            BigDecimal unitPrice = variant.getPriceOverride() != null
                    ? variant.getPriceOverride() : variant.getProduct().getPrice();
            OrderItem item = new OrderItem(storeId, variant, line.quantity(), unitPrice);
            order.addItem(item);
            total = total.add(item.getTotalPrice());
        }
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order get(Long storeId, Long orderId, User requester) {
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        authorize(order, requester);
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> list(Long storeId, User requester, Pageable pageable) {
        if (requester.hasRole("ADMIN") || requester.hasRole("STORE_OWNER")) {
            return orderRepository.findAllByStoreId(storeId, pageable);
        }
        return orderRepository.findAllByStoreIdAndCustomerId(storeId, requester.getId(), pageable);
    }

    public Order cancel(Long storeId, Long orderId, User requester) {
        Order order = get(storeId, orderId, requester);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new BadRequestException("Order cannot be cancelled in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    private void authorize(Order order, User requester) {
        boolean staff = requester.hasRole("ADMIN") || requester.hasRole("STORE_OWNER");
        if (!staff && (order.getCustomerId() == null || !order.getCustomerId().equals(requester.getId()))) {
            throw new ForbiddenException("You do not have access to this order");
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record LineRequest(Long variantId, int quantity) {}
}
