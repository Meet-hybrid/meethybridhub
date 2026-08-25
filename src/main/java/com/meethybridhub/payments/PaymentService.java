package com.meethybridhub.payments;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
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

    public PaymentService(PaymentRepository paymentRepository, OrderService orderService, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
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
        // Webhooks are retried. Reapplying the same terminal state is harmless.
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
