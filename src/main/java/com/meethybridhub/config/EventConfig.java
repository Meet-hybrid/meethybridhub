package com.meethybridhub.config;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Lightweight event infrastructure using Spring's ApplicationEventPublisher.
 * Events are published synchronously by default; listeners annotated with
 * {@code @Async} run on a separate thread pool.
 */
@Configuration
@EnableAsync
public class EventConfig {

    // ─── Base event ─────────────────────────────────────────────────

    public static abstract class DomainEvent extends ApplicationEvent {
        private final Instant occurredAt;

        protected DomainEvent(Object source) {
            super(source);
            this.occurredAt = Instant.now();
        }

        public Instant getOccurredAt() { return occurredAt; }
    }

    // ─── Concrete events ────────────────────────────────────────────

    public static class OrderCreatedEvent extends DomainEvent {
        private final Long orderId;
        private final Long storeId;
        private final Long customerId;

        public OrderCreatedEvent(Object source, Long orderId, Long storeId, Long customerId) {
            super(source);
            this.orderId = orderId;
            this.storeId = storeId;
            this.customerId = customerId;
        }

        public Long getOrderId() { return orderId; }
        public Long getStoreId() { return storeId; }
        public Long getCustomerId() { return customerId; }
    }

    public static class OrderStatusChangedEvent extends DomainEvent {
        private final Long orderId;
        private final String oldStatus;
        private final String newStatus;

        public OrderStatusChangedEvent(Object source, Long orderId, String oldStatus, String newStatus) {
            super(source);
            this.orderId = orderId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        public Long getOrderId() { return orderId; }
        public String getOldStatus() { return oldStatus; }
        public String getNewStatus() { return newStatus; }
    }

    public static class StoreCreatedEvent extends DomainEvent {
        private final Long storeId;
        private final String storeName;

        public StoreCreatedEvent(Object source, Long storeId, String storeName) {
            super(source);
            this.storeId = storeId;
            this.storeName = storeName;
        }

        public Long getStoreId() { return storeId; }
        public String getStoreName() { return storeName; }
    }

    public static class UserRegisteredEvent extends DomainEvent {
        private final Long userId;
        private final String email;

        public UserRegisteredEvent(Object source, Long userId, String email) {
            super(source);
            this.userId = userId;
            this.email = email;
        }

        public Long getUserId() { return userId; }
        public String getEmail() { return email; }
    }

    // ─── Event publisher helper ─────────────────────────────────────

    @Component
    public static class EventPublisher {
        private final ApplicationEventPublisher publisher;

        public EventPublisher(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public void publish(DomainEvent event) {
            publisher.publishEvent(event);
        }
    }
}
