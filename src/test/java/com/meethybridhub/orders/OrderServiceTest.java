package com.meethybridhub.orders;

import com.meethybridhub.orders.dto.OrderDtos;
import com.meethybridhub.orders.dto.OrderDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void testCreateOrderAndProcessPayment() {
        var itemReq = new OrderDtos.CreateOrderItemRequest("var_123", 2, new BigDecimal("49.99"));
        var createOrderReq = new OrderDtos.CreateOrderRequest(
            "store_1",
            "cust_1",
            "123 Main St",
            "123 Main St",
            List.of(itemReq),
            null,
            "Test order"
        );

        Order order = orderService.createOrder(createOrderReq);
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("99.98"));
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");

        var paymentReq = new OrderDtos.CreatePaymentRequest(
            order.getId(),
            "CREDIT_CARD",
            new BigDecimal("99.98"),
            "TX-9999",
            "{}"
        );

        Payment payment = orderService.createPayment(paymentReq);
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo("COMPLETED");

        Order updatedOrder = orderService.getOrderById(order.getId());
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    public void testInstallmentPlanFlow() {
        var itemReq = new OrderDtos.CreateOrderItemRequest("var_100", 1, new BigDecimal("120.00"));
        var createOrderReq = new OrderDtos.CreateOrderRequest(
            "store_1",
            "cust_2",
            "456 Oak St",
            "456 Oak St",
            List.of(itemReq),
            null,
            null
        );

        Order order = orderService.createOrder(createOrderReq);

        var planReq = new OrderDtos.CreateInstallmentPlanRequest(order.getId(), 3);
        InstallmentPlan plan = orderService.createInstallmentPlan(planReq);

        assertThat(plan).isNotNull();
        assertThat(plan.getInstallmentPayments()).hasSize(3);

        var pmtReq = new OrderDtos.ProcessInstallmentPaymentRequest(plan.getId(), new BigDecimal("40.00"));
        InstallmentPayment pmt = orderService.processInstallmentPayment(pmtReq);
        assertThat(pmt.getStatus()).isEqualTo("PAID");
    }
}
