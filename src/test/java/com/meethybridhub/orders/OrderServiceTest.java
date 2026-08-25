package com.meethybridhub.orders;

import com.meethybridhub.catalog.*;
import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.identity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orders;
    @Mock ProductVariantRepository variants;
    @Mock InventoryRepository inventory;

    @Test
    void createsOrderFromVariantPriceAndTrimsOptionalFields() {
        Product product = new Product(7L, "Shoe", null, new BigDecimal("25.00"), null);
        ReflectionTestUtils.setField(product, "id", 10L);
        ProductVariant variant = new ProductVariant(7L, product, "SKU-1", "42", "Black", null);
        ReflectionTestUtils.setField(variant, "id", 11L);
        Inventory stock = new Inventory(7L, variant, 10);
        when(variants.findByIdAndStoreId(11L, 7L)).thenReturn(Optional.of(variant));
        when(inventory.findByStoreIdAndVariantId(7L, 11L)).thenReturn(Optional.of(stock));
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service().create(7L, customer(), " buyer@example.com ", " address ", "  ", " note ",
                List.of(new OrderService.LineRequest(11L, 2)));

        assertThat(result.getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(result.getBillingAddress()).isNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejectsEmptyAndInvalidOrders() {
        assertThatThrownBy(() -> service().create(7L, customer(), "a", "b", null, null, List.of()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service().create(7L, customer(), "a", "b", null, null,
                List.of(new OrderService.LineRequest(1L, 0))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listsAndCancelsOrders() {
        Order order = order(OrderStatus.PENDING);
        when(orders.findByIdAndStoreId(1L, 7L)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);
        when(orders.findAllByStoreId(7L, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(order)));
        assertThat(service().get(7L, 1L, customer())).isSameAs(order);
        assertThat(service().list(7L, owner(), PageRequest.of(0, 10)).getContent()).containsExactly(order);
        assertThat(service().cancel(7L, 1L, owner()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private OrderService service() { return new OrderService(orders, variants, inventory); }
    private User customer() { User u = new User("buyer@example.com", "hash", "Buyer"); ReflectionTestUtils.setField(u, "id", 3L); return u; }
    private User owner() { User u = customer(); u.setRoles("STORE_OWNER"); return u; }
    private Order order(OrderStatus status) { Order o = new Order(7L, customer(), "ORD-1", "buyer@example.com", "addr", null, null); ReflectionTestUtils.setField(o, "id", 1L); o.setStatus(status); o.setTotalAmount(new BigDecimal("10.00")); return o; }
}
