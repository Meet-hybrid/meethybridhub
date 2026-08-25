package com.meethybridhub.payments;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class InstallmentController {

    private final InstallmentService installmentService;
    private final UserService userService;

    public InstallmentController(InstallmentService installmentService, UserService userService) {
        this.installmentService = installmentService;
        this.userService = userService;
    }

    @PostMapping("/orders/{orderId}/installments")
    public ResponseEntity<PlanResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePlanRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        InstallmentPlan plan = installmentService.create(TenantContext.requireStoreId(), orderId, user, request.installmentCount());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanResponse.from(plan));
    }

    @GetMapping("/installments/{planId}")
    public ResponseEntity<PlanResponse> get(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long planId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(PlanResponse.from(installmentService.get(TenantContext.requireStoreId(), planId, user)));
    }

    public record CreatePlanRequest(
            @Min(value = 2, message = "Installment count must be at least 2")
            @Max(value = 12, message = "Installment count cannot exceed 12")
            int installmentCount) {}

    public record PlanResponse(Long id, Long storeId, Long orderId, String orderNumber, BigDecimal totalAmount,
                               int installmentCount, BigDecimal installmentAmount, InstallmentPlanStatus status,
                               List<PaymentResponse> payments, Instant createdAt) {
        static PlanResponse from(InstallmentPlan plan) {
            return new PlanResponse(plan.getId(), plan.getStoreId(), plan.getOrderId(), plan.getOrderNumber(),
                    plan.getTotalAmount(), plan.getInstallmentCount(), plan.getInstallmentAmount(), plan.getStatus(),
                    plan.getPayments().stream().map(PaymentResponse::from).toList(), plan.getCreatedAt());
        }
    }

    public record PaymentResponse(Long id, int sequenceNumber, LocalDate dueDate, BigDecimal amount,
                                  InstallmentPaymentStatus status, Instant paidAt) {
        static PaymentResponse from(InstallmentPayment payment) {
            return new PaymentResponse(payment.getId(), payment.getSequenceNumber(), payment.getDueDate(),
                    payment.getAmount(), payment.getStatus(), payment.getPaidAt());
        }
    }
}
