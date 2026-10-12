package com.meethybridhub.payments;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogService;
import com.meethybridhub.identity.User;
import com.meethybridhub.orders.Order;
import com.meethybridhub.orders.OrderRepository;
import com.meethybridhub.orders.OrderService;
import com.meethybridhub.orders.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository, OrderService orderService,
                          OrderRepository orderRepository, AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
    }

    public Payment initialize(Long storeId, Long orderId, User requester,
                              PaymentMethod method, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String key = idempotencyKey.trim();
        var existing = paymentRepository.findByStoreIdAndIdempotencyKey(storeId, key);
        if (existing.isPresent()) {
            return existing.get();
        }
        Order order = orderService.get(storeId, orderId, requester);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order total must be greater than zero");
        }
        Payment payment = paymentRepository.save(new Payment(storeId, order, method, order.getTotalAmount(), key));
        payment.setTransactionId("KORAPAY-" + payment.getId());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment get(Long storeId, Long paymentId, User requester) {
        Payment payment = paymentRepository.findByIdAndStoreId(paymentId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        orderService.get(storeId, payment.getOrderId(), requester);
        return payment;
    }

    public Payment applyWebhook(Long storeId, String transactionId, String status,
                                BigDecimal amount, String rawPayload) {
        try {
            return processWebhook(storeId, transactionId, status, amount, rawPayload);
        } catch (RuntimeException e) {
            auditLogService.record(null, AuditEventType.PAYMENT_WEBHOOK_REJECTED,
                    "Rejected payment webhook for transaction " + transactionId
                            + " (status=" + status + ", amount=" + amount + "): " + e.getMessage(),
                    null, null);
            throw e;
        }
    }

    private Payment processWebhook(Long storeId, String transactionId, String status,
                                   BigDecimal amount, String rawPayload) {
        Payment payment = paymentRepository.findByStoreIdAndTransactionId(storeId, transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for transaction: " + transactionId));
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new BadRequestException("Webhook amount does not match payment amount");
        }
        PaymentStatus next = switch (status.toLowerCase()) {
            case "success", "successful", "paid" -> PaymentStatus.PAID;
            case "failed", "failure" -> PaymentStatus.FAILED;
            case "refunded" -> PaymentStatus.REFUNDED;
            default -> throw new BadRequestException("Unsupported payment webhook status: " + status);
        };

        // Terminal-state guard: webhooks can be replayed or arrive out of order.
        if (payment.getStatus() == next) {
            // Same-status replay (e.g. duplicate success) is a no-op: no re-save, no order churn.
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.PAID && next == PaymentStatus.FAILED) {
            throw new BadRequestException("Cannot move settled payment from PAID to FAILED");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException("Payment is already refunded; no further status changes are allowed");
        }
        if (next == PaymentStatus.REFUNDED) {
            if (payment.getStatus() != PaymentStatus.PAID) {
                throw new BadRequestException("Cannot refund a payment that is not PAID");
            }
        }

        payment.setStatus(next);
        payment.setGatewayResponse(rawPayload);
        if (next == PaymentStatus.PAID) {
            Order order = orderRepository.findByIdAndStoreId(payment.getStoreId(), payment.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + payment.getOrderId()));
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
            }
        }
        return paymentRepository.save(payment);
    }
}
