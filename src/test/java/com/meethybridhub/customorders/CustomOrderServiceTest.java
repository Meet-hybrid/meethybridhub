package com.meethybridhub.customorders;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserRepository;
import com.meethybridhub.orders.OrderRepository;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOrderServiceTest {

    @Mock private CustomOrderRequestRepository requestRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private CustomOrderConversationRepository conversationRepository;
    @Mock private CustomOrderAttachmentRepository attachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StoreService storeService;

    @InjectMocks private CustomOrderService service;

    private User customer;
    private User storeOwner;
    private CustomOrderRequest request;

    @BeforeEach
    void setUp() throws Exception {
        customer = new User("customer@test.com", "hash", "Test Customer");
        customer.setRoles("CUSTOMER");
        customer.setStatus(User.UserStatus.ACTIVE);
        var cidField = User.class.getDeclaredField("id");
        cidField.setAccessible(true);
        cidField.set(customer, 10L);

        storeOwner = new User("owner@test.com", "hash", "Store Owner");
        storeOwner.setRoles("STORE_OWNER,ADMIN");
        storeOwner.setStatus(User.UserStatus.ACTIVE);
        cidField.set(storeOwner, 20L);

        request = new CustomOrderRequest(1L, customer, "Custom Dress",
                "A bespoke evening dress", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(100000), Instant.now().plusSeconds(86400 * 7));
        request.setId(1L);
    }

    @Test
    void createRequest_savesAndReturns() {
        when(requestRepository.save(any())).thenAnswer(i -> {
            CustomOrderRequest r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        CustomOrderRequest result = service.createRequest(
                customer, 1L, "Custom Dress", "Bespoke evening dress",
                BigDecimal.valueOf(50000), BigDecimal.valueOf(100000),
                Instant.now().plusSeconds(86400 * 7));

        assertNotNull(result);
        assertEquals("Custom Dress", result.getTitle());
        assertEquals(CustomOrderStatus.OPEN, result.getStatus());
        verify(requestRepository).save(any());
    }

    @Test
    void getRequest_throwsWhenNotFound() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getRequest(999L));
    }

    @Test
    void getRequest_returnsExisting() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        CustomOrderRequest result = service.getRequest(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void createQuote_movesRequestToQuoted() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(quoteRepository.save(any())).thenAnswer(i -> {
            Quote q = i.getArgument(0);
            q.setId(1L);
            return q;
        });
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Quote quote = service.createQuote(
                storeOwner, 1L, BigDecimal.valueOf(75000), "NGN",
                14, "Includes fabric and labor", 7);

        assertNotNull(quote);
        assertEquals(QuoteStatus.PENDING, quote.getStatus());
        assertEquals(CustomOrderStatus.QUOTED, request.getStatus());
    }

    @Test
    void createQuote_rejectsIfNotOpenOrInReview() {
        request.setStatus(CustomOrderStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(BadRequestException.class, () ->
                service.createQuote(storeOwner, 1L, BigDecimal.valueOf(75000),
                        "NGN", 14, "terms", 7));
    }

    @Test
    void acceptQuote_setsAcceptedAndRejectsOthers() {
        Quote quote = new Quote(1L, request, BigDecimal.valueOf(75000),
                "NGN", 14, "terms", Instant.now().plusSeconds(86400));
        quote.setId(1L);
        quote.setStatus(QuoteStatus.PENDING);

        Quote otherQuote = new Quote(1L, request, BigDecimal.valueOf(80000),
                "NGN", 10, "other", Instant.now().plusSeconds(86400));
        otherQuote.setId(2L);
        otherQuote.setStatus(QuoteStatus.PENDING);

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quoteRepository.findByRequestIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(quote, otherQuote));
        when(quoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Quote result = service.acceptQuote(customer, 1L);

        assertEquals(QuoteStatus.ACCEPTED, result.getStatus());
        assertEquals(CustomOrderStatus.ACCEPTED, request.getStatus());
        assertEquals(QuoteStatus.REJECTED, otherQuote.getStatus());
    }

    @Test
    void acceptQuote_rejectsIfNotCustomer() {
        Quote quote = new Quote(1L, request, BigDecimal.valueOf(75000),
                "NGN", 14, "terms", Instant.now().plusSeconds(86400));
        quote.setId(1L);
        quote.setStatus(QuoteStatus.PENDING);

        User otherUser = new User("other@test.com", "hash", "Other");
        otherUser.setRoles("CUSTOMER");

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

        assertThrows(ForbiddenException.class, () -> service.acceptQuote(otherUser, 1L));
    }

    @Test
    void rejectQuote_setsRejected() {
        Quote quote = new Quote(1L, request, BigDecimal.valueOf(75000),
                "NGN", 14, "terms", Instant.now().plusSeconds(86400));
        quote.setId(1L);
        quote.setStatus(QuoteStatus.PENDING);

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Quote result = service.rejectQuote(customer, 1L);
        assertEquals(QuoteStatus.REJECTED, result.getStatus());
    }

    @Test
    void sendMessage_createsMessageAndMovesToInReview() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(conversationRepository.save(any())).thenAnswer(i -> {
            CustomOrderConversation m = i.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CustomOrderConversation msg = service.sendMessage(customer, 1L, "Hello, I'm interested");

        assertNotNull(msg);
        assertEquals(CustomOrderStatus.IN_REVIEW, request.getStatus());
    }

    @Test
    void convertToOrder_throwsIfNotAccepted() {
        request.setStatus(CustomOrderStatus.OPEN);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(BadRequestException.class, () -> service.convertToOrder(1L));
    }

    @Test
    void convertToOrder_createsOrderAndMarksConverted() {
        request.setStatus(CustomOrderStatus.ACCEPTED);
        Quote acceptedQuote = new Quote(1L, request, BigDecimal.valueOf(75000),
                "NGN", 14, "terms", Instant.now().plusSeconds(86400));
        acceptedQuote.setId(1L);
        acceptedQuote.setStatus(QuoteStatus.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(acceptedQuote));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(orderRepository.save(any())).thenAnswer(i -> {
            var order = i.getArgument(0);
            var idField = order.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, 1L);
            return order;
        });
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var order = service.convertToOrder(1L);

        assertEquals(CustomOrderStatus.CONVERTED, request.getStatus());
        assertNotNull(order);
    }

    @Test
    void listRequestsForStore_filtersByStatus() {
        when(requestRepository.findByStoreIdAndStatus(1L, CustomOrderStatus.OPEN))
                .thenReturn(List.of(request));

        List<CustomOrderRequest> result = service.listRequestsForStore(1L, "OPEN");
        assertEquals(1, result.size());
    }

    @Test
    void listRequestsForStore_throwsOnBadStatus() {
        assertThrows(BadRequestException.class,
                () -> service.listRequestsForStore(1L, "BOGUS"));
    }
}
