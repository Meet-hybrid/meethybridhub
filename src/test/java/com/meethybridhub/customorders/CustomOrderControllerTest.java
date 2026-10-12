package com.meethybridhub.customorders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meethybridhub.common.api.GlobalExceptionHandler;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.orders.Order;
import com.meethybridhub.store.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomOrderControllerTest {

    @Mock
    private CustomOrderService service;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    private User customer;
    private User storeOwner;

    @BeforeEach
    void setUp() {
        customer = user(10L, "customer@test.com", "CUSTOMER");
        storeOwner = user(20L, "owner@test.com", "STORE_OWNER");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CustomOrderController(service, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestPrincipalResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRequestReturns201AndForwardsTenantStoreId() throws Exception {
        TenantContext.setStoreId(1L);
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.createRequest(any(), anyLong(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(request());

        mockMvc.perform(post("/api/v1/custom-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"title":"Custom Dress","description":"Bespoke evening dress",
                                 "budgetMin":50000,"budgetMax":100000}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Custom Dress"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(service).createRequest(eq(customer), eq(1L), eq("Custom Dress"),
                eq("Bespoke evening dress"), eq(new BigDecimal("50000")),
                eq(new BigDecimal("100000")), isNull());
    }

    @Test
    void createRequestWithoutStoreContextReturns400() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);

        mockMvc.perform(post("/api/v1/custom-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"title":"T","description":"D"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRequestReturnsRequest() throws Exception {
        when(service.getRequest(1L)).thenReturn(request());

        mockMvc.perform(get("/api/v1/custom-orders/1")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerId").value(10L))
                .andExpect(jsonPath("$.budgetMin").value(50000));
    }

    @Test
    void getRequestUnknownIdReturns404() throws Exception {
        when(service.getRequest(999L)).thenThrow(new ResourceNotFoundException("Custom order request not found: 999"));

        mockMvc.perform(get("/api/v1/custom-orders/999")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listRequestsAsStoreOwnerUsesStoreListing() throws Exception {
        TenantContext.setStoreId(1L);
        when(userService.getUserByEmail("owner@test.com")).thenReturn(storeOwner);
        when(service.listRequestsForStore(1L, "OPEN")).thenReturn(List.of(request()));

        mockMvc.perform(get("/api/v1/custom-orders")
                        .param("status", "OPEN")
                        .principal(principal("owner@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(service).listRequestsForStore(1L, "OPEN");
    }

    @Test
    void listRequestsAsCustomerUsesCustomerListing() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.listRequestsForCustomer(10L)).thenReturn(List.of(request()));

        mockMvc.perform(get("/api/v1/custom-orders")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Custom Dress"));

        verify(service).listRequestsForCustomer(10L);
    }

    @Test
    void updateStatusReturnsUpdatedRequest() throws Exception {
        when(userService.getUserByEmail("owner@test.com")).thenReturn(storeOwner);
        CustomOrderRequest inReview = request();
        inReview.setStatus(CustomOrderStatus.IN_REVIEW);
        when(service.updateStatus(eq(storeOwner), eq(1L), eq(CustomOrderStatus.IN_REVIEW))).thenReturn(inReview);

        mockMvc.perform(put("/api/v1/custom-orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("owner@test.com"))
                        .content("""
                                {"status":"IN_REVIEW"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    void createQuoteReturns201() throws Exception {
        when(userService.getUserByEmail("owner@test.com")).thenReturn(storeOwner);
        when(service.createQuote(eq(storeOwner), eq(1L), eq(new BigDecimal("75000")),
                eq("NGN"), eq(14), eq("Includes fabric"), eq(7))).thenReturn(quote());

        mockMvc.perform(post("/api/v1/custom-orders/1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("owner@test.com"))
                        .content("""
                                {"price":75000,"currency":"NGN","estimatedDays":14,
                                 "terms":"Includes fabric","expiryDays":7}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createQuoteWithMissingPriceReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/custom-orders/1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("owner@test.com"))
                        .content("""
                                {"currency":"NGN","estimatedDays":14,"terms":"t","expiryDays":7}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listQuotesReturnsQuotes() throws Exception {
        when(service.listQuotesForRequest(1L)).thenReturn(List.of(quote()));

        mockMvc.perform(get("/api/v1/custom-orders/1/quotes")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(75000))
                .andExpect(jsonPath("$[0].currency").value("NGN"));
    }

    @Test
    void acceptQuoteReturnsAcceptedQuote() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        Quote accepted = quote();
        accepted.setStatus(QuoteStatus.ACCEPTED);
        when(service.acceptQuote(customer, 1L)).thenReturn(accepted);

        mockMvc.perform(post("/api/v1/custom-orders/quotes/1/accept")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectQuoteReturnsRejectedQuote() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        Quote rejected = quote();
        rejected.setStatus(QuoteStatus.REJECTED);
        when(service.rejectQuote(customer, 1L)).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/custom-orders/quotes/1/reject")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void sendMessageReturns201() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.sendMessage(eq(customer), eq(1L), eq("Hello, I'm interested"))).thenReturn(message());

        mockMvc.perform(post("/api/v1/custom-orders/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"message":"Hello, I'm interested"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value(10L))
                .andExpect(jsonPath("$.message").value("Hello, I'm interested"));
    }

    @Test
    void sendMessageWithBlankMessageReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/custom-orders/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"message":"  "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMessagesReturnsMessages() throws Exception {
        when(service.listMessages(1L)).thenReturn(List.of(message()));

        mockMvc.perform(get("/api/v1/custom-orders/1/messages")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Hello, I'm interested"));
    }

    @Test
    void addAttachmentReturns201() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.addAttachment(eq(customer), eq(1L), eq("https://cdn.test/sketch.png"),
                eq("sketch.png"), eq("image/png"), eq(2048L))).thenReturn(attachment());

        mockMvc.perform(post("/api/v1/custom-orders/1/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"fileUrl":"https://cdn.test/sketch.png","fileName":"sketch.png",
                                 "fileType":"image/png","fileSizeBytes":2048}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("sketch.png"))
                .andExpect(jsonPath("$.fileSizeBytes").value(2048));
    }

    @Test
    void addAttachmentMissingFileUrlReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/custom-orders/1/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal("customer@test.com"))
                        .content("""
                                {"fileName":"sketch.png"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAttachmentsReturnsAttachments() throws Exception {
        when(service.listAttachments(1L)).thenReturn(List.of(attachment()));

        mockMvc.perform(get("/api/v1/custom-orders/1/attachments")
                        .principal(principal("customer@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileUrl").value("https://cdn.test/sketch.png"));
    }

    @Test
    void convertToOrderReturnsOrderSummary() throws Exception {
        when(service.convertToOrder(1L)).thenReturn(order());

        mockMvc.perform(post("/api/v1/custom-orders/1/convert")
                        .principal(principal("owner@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
                .andExpect(jsonPath("$.message").value("Custom order converted to order successfully"));
    }

    // --- fixtures ---

    private User user(Long id, String email, String roles) {
        User u = new User(email, "hash", "Test User");
        u.setRoles(roles);
        setField(u, "id", id);
        return u;
    }

    private CustomOrderRequest request() {
        CustomOrderRequest r = new CustomOrderRequest(1L, customer, "Custom Dress",
                "Bespoke evening dress", BigDecimal.valueOf(50000), BigDecimal.valueOf(100000),
                Instant.parse("2026-10-20T00:00:00Z"));
        r.setId(1L);
        setField(r, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return r;
    }

    private Quote quote() {
        Quote q = new Quote(1L, request(), BigDecimal.valueOf(75000), "NGN", 14,
                "Includes fabric", Instant.parse("2026-10-19T00:00:00Z"));
        q.setId(1L);
        setField(q, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return q;
    }

    private CustomOrderConversation message() {
        CustomOrderConversation m = new CustomOrderConversation(request(), customer, "Hello, I'm interested");
        m.setId(1L);
        setField(m, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return m;
    }

    private CustomOrderAttachment attachment() {
        CustomOrderAttachment a = new CustomOrderAttachment(request(), customer,
                "https://cdn.test/sketch.png", "sketch.png", "image/png", 2048L);
        setField(a, "id", 1L);
        setField(a, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return a;
    }

    private Order order() {
        Order o = new Order(1L, customer, "ORD-1", "customer@test.com", "addr", null, null);
        setField(o, "id", 1L);
        return o;
    }

    private PrincipalUserDetails principal(String email) {
        return new PrincipalUserDetails(email);
    }

    /** Test principal that satisfies both {@code java.security.Principal} (request) and
     *  {@code UserDetails} ({@code @AuthenticationPrincipal} resolution). */
    static class PrincipalUserDetails implements UserDetails, java.security.Principal {
        private final String username;

        PrincipalUserDetails(String username) {
            this.username = username;
        }

        @Override
        public java.util.Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return java.util.List.of();
        }

        @Override
        public String getPassword() { return "n/a"; }

        @Override
        public String getUsername() { return username; }

        @Override
        public boolean isAccountNonExpired() { return true; }

        @Override
        public boolean isAccountNonLocked() { return true; }

        @Override
        public boolean isCredentialsNonExpired() { return true; }

        @Override
        public boolean isEnabled() { return true; }

        @Override
        public String getName() { return username; }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException("No field " + name + " on " + type);
    }

    /**
     * Standalone MockMvc has no Spring Security wiring, so resolve
     * @AuthenticationPrincipal from the request principal directly.
     */
    static class TestPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return webRequest.getNativeRequest(
                    jakarta.servlet.http.HttpServletRequest.class).getUserPrincipal();
        }
    }
}
