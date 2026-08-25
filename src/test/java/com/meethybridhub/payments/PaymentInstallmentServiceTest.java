package com.meethybridhub.payments;

import com.meethybridhub.identity.User;
import com.meethybridhub.orders.Order;
import com.meethybridhub.orders.OrderRepository;
import com.meethybridhub.orders.OrderService;
import com.meethybridhub.orders.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInstallmentServiceTest {
    @Mock PaymentRepository payments;
    @Mock OrderRepository orders;
    @Mock OrderService ordersService;
    @Mock InstallmentPlanRepository plans;

    @Test
    void initializesIdempotentPaymentAndProcessesPaidWebhook() {
        User requester = user();
        Order order = order();
        when(payments.findByStoreIdAndIdempotencyKey(7L, "key")).thenReturn(Optional.empty());
        when(ordersService.get(7L, 1L, requester)).thenReturn(order);
        when(payments.save(any(Payment.class))).thenAnswer(i -> { Payment p = i.getArgument(0); ReflectionTestUtils.setField(p, "id", 4L); return p; });
        Payment payment = new PaymentService(payments, ordersService, orders).initialize(7L, 1L, requester, PaymentMethod.CARD, " key ");
        assertThat(payment.getTransactionId()).isEqualTo("KORAPAY-4");

        when(payments.findByStoreIdAndTransactionId(7L, "KORAPAY-4")).thenReturn(Optional.of(payment));
        when(orders.findByIdAndStoreId(7L, 1L)).thenReturn(Optional.of(order));
        Payment result = new PaymentService(payments, ordersService, orders)
                .applyWebhook(7L, "KORAPAY-4", "success", new BigDecimal("20.00"), "{}");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void createsInstallmentPaymentsWithRemainderInFinalPayment() {
        User requester = user();
        Order order = order();
        when(ordersService.get(7L, 1L, requester)).thenReturn(order);
        when(plans.findByStoreIdAndOrder_Id(7L, 1L)).thenReturn(Optional.empty());
        when(plans.save(any(InstallmentPlan.class))).thenAnswer(i -> i.getArgument(0));
        InstallmentPlan plan = new InstallmentService(plans, ordersService).create(7L, 1L, requester, 3);
        assertThat(plan.getPayments()).hasSize(3);
        assertThat(plan.getPayments().get(2).getAmount()).isEqualByComparingTo("6.66");
        assertThat(plan.getPayments().get(0).getPlanId()).isNull();
    }

    private User user() { User u = new User("buyer@example.com", "hash", "Buyer"); ReflectionTestUtils.setField(u, "id", 2L); return u; }
    private Order order() { Order o = new Order(7L, user(), "ORD-1", "buyer@example.com", "addr", null, null); ReflectionTestUtils.setField(o, "id", 1L); o.setTotalAmount(new BigDecimal("20.00")); return o; }
}
