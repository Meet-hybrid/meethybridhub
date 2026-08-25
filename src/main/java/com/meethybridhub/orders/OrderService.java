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
import com.meethybridhub.orders.dto.OrderDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InstallmentPlanRepository installmentPlanRepository;
    private final InstallmentPaymentRepository installmentPaymentRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final TaxRateRepository taxRateRepository;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        PaymentRepository paymentRepository,
        InstallmentPlanRepository installmentPlanRepository,
        InstallmentPaymentRepository installmentPaymentRepository,
        ShippingMethodRepository shippingMethodRepository,
        TaxRateRepository taxRateRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.installmentPaymentRepository = installmentPaymentRepository;
        this.shippingMethodRepository = shippingMethodRepository;
        this.taxRateRepository = taxRateRepository;
    }

    public Order createOrder(CreateOrderRequest req) {
        Order order = new Order();
        order.setStoreId(req.storeId());
        order.setCustomerId(req.customerId());
        order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + ((int) (Math.random() * 9000) + 1000));
        order.setShippingAddress(req.shippingAddress());
        order.setBillingAddress(req.billingAddress());
        order.setNotes(req.notes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemReq : req.items()) {
            OrderItem item = new OrderItem();
            item.setProductVariantId(itemReq.productVariantId());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            BigDecimal itemTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            item.setTotalPrice(itemTotal);
            order.addItem(item);
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal shippingAmount = BigDecimal.ZERO;
        if (req.shippingMethodId() != null) {
            shippingAmount = shippingMethodRepository.findById(req.shippingMethodId())
                .map(ShippingMethod::getCost)
                .orElse(BigDecimal.ZERO);
        }

        BigDecimal totalAmount = subtotal.add(shippingAmount);
        order.setShippingAmount(shippingAmount);
        order.setTotalAmount(totalAmount);

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
    public List<Order> getOrders(String storeId, String customerId) {
        if (storeId != null && customerId != null) {
            return orderRepository.findByStoreIdAndCustomerId(storeId, customerId);
        } else if (storeId != null) {
            return orderRepository.findByStoreId(storeId);
        } else if (customerId != null) {
            return orderRepository.findByCustomerId(customerId);
        }
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(String id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order cancelOrder(String id) {
        Order order = getOrderById(id);
        if (order != null) {
            order.setStatus("CANCELLED");
            return orderRepository.save(order);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(String orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Payments
    public Payment createPayment(CreatePaymentRequest req) {
        Payment payment = new Payment();
        payment.setOrderId(req.orderId());
        payment.setPaymentMethod(req.paymentMethod());
        payment.setAmount(req.amount());
        payment.setTransactionId(req.transactionId() != null ? req.transactionId() : "TX-" + UUID.randomUUID());
        payment.setPaymentGatewayResponse(req.paymentGatewayResponse());
        payment.setStatus("COMPLETED");

        Payment saved = paymentRepository.save(payment);

        Order order = getOrderById(req.orderId());
        if (order != null) {
            order.setPaymentStatus("PAID");
            order.setStatus("PROCESSING");
            orderRepository.save(order);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(String id) {
        return paymentRepository.findById(id).orElse(null);
    }

    public Payment handleWebhook(PaymentWebhookRequest req) {
        Payment payment = getPaymentById(req.paymentId());
        if (payment != null) {
            payment.setStatus(req.status().toUpperCase());
            if (req.transactionId() != null) payment.setTransactionId(req.transactionId());
            if (req.gatewayResponse() != null) payment.setPaymentGatewayResponse(req.gatewayResponse());
            
            if ("SUCCEEDED".equalsIgnoreCase(req.status())) {
                Order order = getOrderById(payment.getOrderId());
                if (order != null) {
                    order.setPaymentStatus("PAID");
                    orderRepository.save(order);
                }
            }
            return paymentRepository.save(payment);
        }
        return null;
    }

    // Installments
    public InstallmentPlan createInstallmentPlan(CreateInstallmentPlanRequest req) {
        Order order = getOrderById(req.orderId());
        if (order == null) throw new IllegalArgumentException("Order not found: " + req.orderId());

        InstallmentPlan plan = new InstallmentPlan();
        plan.setOrderId(req.orderId());
        plan.setTotalAmount(order.getTotalAmount());
        plan.setInstallmentCount(req.installmentCount());

        BigDecimal installmentAmount = order.getTotalAmount().divide(BigDecimal.valueOf(req.installmentCount()), 2, RoundingMode.HALF_UP);
        plan.setInstallmentAmount(installmentAmount);

        Instant startDate = Instant.now();
        for (int i = 0; i < req.installmentCount(); i++) {
            InstallmentPayment pmt = new InstallmentPayment();
            pmt.setAmount(installmentAmount);
            pmt.setDueDate(startDate.plus(30L * i, ChronoUnit.DAYS));
            pmt.setStatus("PENDING");
            plan.addInstallmentPayment(pmt);
        }

        return installmentPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<InstallmentPlan> getInstallmentPlans(String orderId) {
        if (orderId != null) {
            return installmentPlanRepository.findByOrderId(orderId);
        }
        return installmentPlanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public InstallmentPlan getInstallmentPlanById(String id) {
        return installmentPlanRepository.findById(id).orElse(null);
    }

    public InstallmentPayment processInstallmentPayment(ProcessInstallmentPaymentRequest req) {
        List<InstallmentPayment> pmts = installmentPaymentRepository.findByInstallmentPlanIdOrderByDueDateAsc(req.installmentPlanId());
        InstallmentPayment pendingPmt = pmts.stream()
            .filter(p -> "PENDING".equals(p.getStatus()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No pending installment payment found"));

        pendingPmt.setStatus("PAID");
        pendingPmt.setPaidAt(Instant.now());
        InstallmentPayment saved = installmentPaymentRepository.save(pendingPmt);

        boolean allPaid = pmts.stream().allMatch(p -> "PAID".equals(p.getStatus()) || p.getId().equals(saved.getId()));
        if (allPaid) {
            InstallmentPlan plan = getInstallmentPlanById(req.installmentPlanId());
            if (plan != null) {
                plan.setStatus("COMPLETED");
                installmentPlanRepository.save(plan);
            }
        }

        return saved;
    }

    // Shipping & Tax
    @Transactional(readOnly = true)
    public List<ShippingMethod> calculateShipping(String storeId, String country, String state) {
        return shippingMethodRepository.findByStoreIdAndEnabledTrue(storeId);
    }

    @Transactional(readOnly = true)
    public List<ShippingMethod> getShippingMethods(String storeId) {
        return shippingMethodRepository.findByStoreIdAndEnabledTrue(storeId);
    }

    @Transactional(readOnly = true)
    public TaxCalculationResult calculateTax(String storeId, String country, BigDecimal amount, String state) {
        BigDecimal rate = taxRateRepository.findFirstByStoreIdAndCountryAndStateOrderByStateDesc(storeId, country, state)
            .map(TaxRate::getRate)
            .orElse(BigDecimal.ZERO);

        BigDecimal taxAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new TaxCalculationResult(storeId, country, state, amount, rate, taxAmount);
    }

    @Transactional(readOnly = true)
    public List<TaxRate> getTaxRates(String storeId) {
        return taxRateRepository.findByStoreId(storeId);
    }
}
