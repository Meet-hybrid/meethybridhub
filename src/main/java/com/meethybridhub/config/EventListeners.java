package com.meethybridhub.config;

import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous listeners for domain events. Each method runs on a separate
 * thread so the event publisher is never blocked.
 */
@Component
public class EventListeners {

    private static final Logger log = LoggerFactory.getLogger(EventListeners.class);

    private final AuditLogService auditLogService;

    public EventListeners(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Async
    @EventListener
    public void onOrderCreated(EventConfig.OrderCreatedEvent event) {
        log.info("Event: OrderCreated — order {} for store {}", event.getOrderId(), event.getStoreId());
        auditLogService.record(event.getCustomerId(), AuditEventType.LOGIN_SUCCESS,
                "Order created: " + event.getOrderId(), null, null);
    }

    @Async
    @EventListener
    public void onOrderStatusChanged(EventConfig.OrderStatusChangedEvent event) {
        log.info("Event: OrderStatusChanged — order {} {} → {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
    }

    @Async
    @EventListener
    public void onStoreCreated(EventConfig.StoreCreatedEvent event) {
        log.info("Event: StoreCreated — {} ({})", event.getStoreName(), event.getStoreId());
    }

    @Async
    @EventListener
    public void onUserRegistered(EventConfig.UserRegisteredEvent event) {
        log.info("Event: UserRegistered — {} ({})", event.getEmail(), event.getUserId());
        auditLogService.record(event.getUserId(), AuditEventType.REGISTER,
                "User registered: " + event.getEmail(), null, null);
    }
}
