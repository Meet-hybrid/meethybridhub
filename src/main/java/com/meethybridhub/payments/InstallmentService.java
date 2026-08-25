package com.meethybridhub.payments;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.identity.User;
import com.meethybridhub.orders.Order;
import com.meethybridhub.orders.OrderService;
import com.meethybridhub.orders.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
public class InstallmentService {

    private final InstallmentPlanRepository planRepository;
    private final OrderService orderService;

    public InstallmentService(InstallmentPlanRepository planRepository, OrderService orderService) {
        this.planRepository = planRepository;
        this.orderService = orderService;
    }

    public InstallmentPlan create(Long storeId, Long orderId, User requester, int count) {
        Order order = orderService.get(storeId, orderId, requester);
        if (order.getCustomerId() == null) {
            throw new BadRequestException("Installment payments require a customer account");
        }
        if (count < 2 || count > 12) {
            throw new BadRequestException("Installment count must be between 2 and 12");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot create installments for a cancelled order");
        }
        if (planRepository.findByStoreIdAndOrderId(storeId, orderId).isPresent()) {
            throw new BadRequestException("An installment plan already exists for this order");
        }
        BigDecimal amount = order.getTotalAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.CEILING);
        InstallmentPlan plan = new InstallmentPlan(storeId, order, count, amount);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int sequence = 1; sequence <= count; sequence++) {
            BigDecimal paymentAmount = sequence == count ? order.getTotalAmount().subtract(allocated) : amount;
            allocated = allocated.add(paymentAmount);
            plan.addPayment(new InstallmentPayment(storeId, sequence, LocalDate.now().plusMonths(sequence), paymentAmount));
        }
        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public InstallmentPlan get(Long storeId, Long planId, User requester) {
        InstallmentPlan plan = planRepository.findByIdAndStoreId(planId, storeId)
                .orElseThrow(() -> new com.meethybridhub.common.exception.ResourceNotFoundException("Installment plan not found: " + planId));
        orderService.get(storeId, plan.getOrderId(), requester);
        return plan;
    }
}
