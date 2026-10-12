package com.meethybridhub.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meethybridhub.common.api.GlobalExceptionHandler;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiscoveryControllerTest {

    @Mock
    private DiscoveryService service;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User("customer@test.com", "hash", "Test Customer");
        setField(customer, "id", 10L);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DiscoveryController(service, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestPrincipalResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void createStoreReviewReturns201() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.createStoreReview(eq(customer), eq(1L), eq((short) 5),
                eq("Great store"), eq("Loved it"))).thenReturn(storeReview());

        mockMvc.perform(post("/api/v1/discovery/stores/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal())
                        .content("""
                                {"rating":5,"title":"Great store","comment":"Loved it"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Great store"))
                .andExpect(jsonPath("$.customerId").value(10L));
    }

    @Test
    void createStoreReviewWithInvalidRatingReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/stores/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal())
                        .content("""
                                {"rating":9,"title":"t","comment":"c"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listStoreReviewsReturnsMappedResponses() throws Exception {
        when(service.listStoreReviews(1L)).thenReturn(List.of(storeReview()));

        mockMvc.perform(get("/api/v1/discovery/stores/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Great store"))
                .andExpect(jsonPath("$[0].verified").value(true));
    }

    @Test
    void storeReviewSummaryReturnsMap() throws Exception {
        when(service.getStoreReviewSummary(1L)).thenReturn(Map.of("averageRating", 4.5, "count", 12));

        mockMvc.perform(get("/api/v1/discovery/stores/1/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(12));
    }

    @Test
    void createProductReviewReturns201() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.createProductReview(eq(customer), eq(3L), eq(7L), eq((short) 4),
                eq("Nice product"), eq("Good quality"))).thenReturn(productReview());

        mockMvc.perform(post("/api/v1/discovery/products/7/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal())
                        .content("""
                                {"storeId":3,"rating":4,"title":"Nice product","comment":"Good quality"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(7L))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void listProductReviewsReturnsMappedResponses() throws Exception {
        when(service.listProductReviews(7L)).thenReturn(List.of(productReview()));

        mockMvc.perform(get("/api/v1/discovery/products/7/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(7L));
    }

    @Test
    void addFavoriteReturns201() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);

        mockMvc.perform(post("/api/v1/discovery/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal())
                        .content("""
                                {"entityType":"STORE","entityId":1}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Added to favorites"));

        verify(service).addFavorite(customer, FavoriteEntityType.STORE, 1L);
    }

    @Test
    void removeFavoriteReturns200() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);

        mockMvc.perform(delete("/api/v1/discovery/favorites/PRODUCT/7")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Removed from favorites"));

        verify(service).removeFavorite(customer, FavoriteEntityType.PRODUCT, 7L);
    }

    @Test
    void listFavoritesReturnsFavorites() throws Exception {
        when(userService.getUserByEmail("customer@test.com")).thenReturn(customer);
        when(service.listFavorites(customer, FavoriteEntityType.STORE))
                .thenReturn(List.of(favorite()));

        mockMvc.perform(get("/api/v1/discovery/favorites")
                        .param("entityType", "STORE")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(1L));
    }

    @Test
    void featuredStoresReturnsList() throws Exception {
        when(service.getFeaturedStores()).thenReturn(List.of(featured()));

        mockMvc.perform(get("/api/v1/discovery/featured/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Featured Store"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void featuredProductsReturnsList() throws Exception {
        when(service.getFeaturedProducts()).thenReturn(List.of(featured()));

        mockMvc.perform(get("/api/v1/discovery/featured/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sortOrder").value(1));
    }

    @Test
    void featuredBannersReturnsList() throws Exception {
        when(service.getActiveBanners()).thenReturn(List.of(featured()));

        mockMvc.perform(get("/api/v1/discovery/featured/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value("https://cdn.test/banner.png"));
    }

    @Test
    void allFeaturedReturnsList() throws Exception {
        when(service.getAllFeatured()).thenReturn(List.of(featured()));

        mockMvc.perform(get("/api/v1/discovery/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchStoresDelegatesWithDefaults() throws Exception {
        when(service.searchStores(eq("shoes"), eq(0), eq(20))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/discovery/search/stores")
                        .param("q", "shoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).searchStores("shoes", 0, 20);
    }

    @Test
    void searchStoresWithPagingParams() throws Exception {
        when(service.searchStores(eq("dress"), eq(2), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/discovery/search/stores")
                        .param("q", "dress")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(service).searchStores("dress", 2, 10);
    }

    // --- fixtures ---

    private StoreReview storeReview() {
        StoreReview r = new StoreReview(1L, customer, (short) 5, "Great store", "Loved it", true);
        setField(r, "id", 1L);
        setField(r, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return r;
    }

    private ProductReview productReview() {
        ProductReview r = new ProductReview(3L, 7L, customer, (short) 4,
                "Nice product", "Good quality", false);
        setField(r, "id", 2L);
        setField(r, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return r;
    }

    private UserFavorite favorite() {
        UserFavorite f = new UserFavorite(10L, FavoriteEntityType.STORE, 1L);
        setField(f, "id", 1L);
        setField(f, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return f;
    }

    private FeaturedContent featured() {
        FeaturedContent c = new FeaturedContent(FeaturedContentType.STORE, "Featured Store",
                "Top rated", "https://cdn.test/banner.png", "https://shop.test", 1);
        setField(c, "id", 1L);
        setField(c, "createdAt", Instant.parse("2026-10-12T00:00:00Z"));
        return c;
    }

    private PrincipalUserDetails principal() {
        return new PrincipalUserDetails("customer@test.com");
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findField(Class<?> type, String name) {
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

    static class PrincipalUserDetails implements UserDetails, java.security.Principal {
        private final String username;

        PrincipalUserDetails(String username) {
            this.username = username;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of();
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
