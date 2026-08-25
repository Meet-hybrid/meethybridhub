package com.meethybridhub.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class OrderDtos {

    public record CreateOrderItemRequest(
        @NotBlank String productVariantId,
        @NotNull @Min(1) Integer quantity,
        @NotNull BigDecimal unitPrice
    ) {}

    public record CreateOrderRequest(
        @NotBlank String storeId,
        @NotBlank String customerId,
        @NotBlank String shippingAddress,
        @NotBlank String billingAddress,
        @NotEmpty List<CreateOrderItemRequest> items,
        String shippingMethodId,
        String notes
    ) {}

    public record CreatePaymentRequest(
        @NotBlank String orderId,
        @NotBlank String paymentMethod,
        @NotNull BigDecimal amount,
        String transactionId,
        String paymentGatewayResponse
    ) {}

    public record PaymentWebhookRequest(
        @NotBlank String paymentId,
        @NotBlank String status,
        String transactionId,
        String gatewayResponse
    ) {}

    public record CreateInstallmentPlanRequest(
        @NotBlank String orderId,
        @NotNull @Min(2) Integer installmentCount
    ) {}

    public record ProcessInstallmentPaymentRequest(
        @NotBlank String installmentPlanId,
        @NotNull BigDecimal amount
    ) {}

    public record TaxCalculationResult(
        String storeId,
        String country,
        String state,
        BigDecimal amount,
        BigDecimal taxRate,
        BigDecimal taxAmount
    ) {}
}
