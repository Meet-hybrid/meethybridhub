package com.meethybridhub.store;

import com.meethybridhub.identity.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link StoreFilter} — no Spring context.
 *
 * TenantContext is cleared in the filter's finally block, so we capture it
 * inside the filterChain callback rather than checking after doFilterInternal.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StoreFilterTest {

    @Mock private StoreRepository storeRepository;
    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private StoreFilter filter;

    private static final String BASE_DOMAIN = "meethybridhub.com";

    @BeforeEach
    void setUp() {
        filter = new StoreFilter(storeRepository, jwtService, BASE_DOMAIN);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /** Capture TenantContext inside the filterChain callback, before the finally block clears it. */
    private Long[] captureTenantContext() throws ServletException, IOException {
        Long[] captured = new Long[1];
        doAnswer(inv -> {
            captured[0] = TenantContext.getStoreId().orElse(null);
            return null;
        }).when(filterChain).doFilter(request, response);
        return captured;
    }

    // ── shouldNotFilter ──────────────────────────────────────────

    @Test
    void shouldNotFilterLogin() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterRegister() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/register");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterRefresh() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/refresh");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterLogout() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/logout");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterVerify() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/verify");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterResetPassword() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/reset-password/request");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterActuator() {
        when(request.getServletPath()).thenReturn("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterSwaggerUi() {
        when(request.getServletPath()).thenReturn("/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterApiDocs() {
        when(request.getServletPath()).thenReturn("/v3/api-docs");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldFilterProtectedEndpoints() {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ── Resolution: X-Store-Id header ────────────────────────────

    @Test
    void resolvesFromStoreIdHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("42");
        when(storeRepository.existsById(42L)).thenReturn(true);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(42L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresNonExistentStoreIdHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("999");
        when(storeRepository.existsById(999L)).thenReturn(false);
        // Falls through — slug returns empty
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        // Subdomain returns empty
        when(request.getHeader("Host")).thenReturn("localhost");
        // JWT returns empty
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void ignoresNonNumericStoreIdHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("not-a-number");
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void ignoresBlankStoreIdHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("   ");
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    // ── Resolution: X-Store-Slug header ──────────────────────────

    @Test
    void resolvesFromSlugHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn("divinez-signature");
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(10L);
        when(store.getStatus()).thenReturn(StoreStatus.ACTIVE);
        when(storeRepository.findBySlug("divinez-signature")).thenReturn(Optional.of(store));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(10L);
    }

    @Test
    void slugHeaderIgnoredForInactiveStore() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn("divinez-signature");
        Store store = mock(Store.class);
        when(store.getStatus()).thenReturn(StoreStatus.SUSPENDED);
        when(storeRepository.findBySlug("divinez-signature")).thenReturn(Optional.of(store));
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    // ── Resolution: subdomain ────────────────────────────────────

    @Test
    void resolvesFromSubdomain() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("divinez-signature.meethybridhub.com");
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(20L);
        when(storeRepository.findBySlug("divinez-signature")).thenReturn(Optional.of(store));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(20L);
    }

    @Test
    void ignoresBareBaseDomain() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("meethybridhub.com");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void ignoresMultiLabelSubdomain() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("a.b.meethybridhub.com");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void ignoresLocalhostHost() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost:3000");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void ignoresNullHost() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void resolvesSubdomainWithPort() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("mystore.meethybridhub.com:3000");
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(30L);
        when(storeRepository.findBySlug("mystore")).thenReturn(Optional.of(store));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(30L);
    }

    // ── Resolution: JWT storeId claim ────────────────────────────

    @Test
    void resolvesFromJwtStoreIdClaim() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn("Bearer some-jwt");
        when(jwtService.extractClaim(eq("some-jwt"), any())).thenReturn(50L);
        when(storeRepository.existsById(50L)).thenReturn(true);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(50L);
    }

    @Test
    void jwtResolutionSkippedWhenNoStoreIdClaim() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn("Bearer some-jwt");
        when(jwtService.extractClaim(eq("some-jwt"), any())).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void jwtResolutionSkippedWhenJwtThrows() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-jwt");
        when(jwtService.extractClaim(eq("bad-jwt"), any())).thenThrow(new RuntimeException("invalid"));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void jwtResolutionSkippedWhenNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn(null);
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void jwtResolutionSkippedForBasicAuth() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isNull();
    }

    // ── Context cleanup ──────────────────────────────────────────

    @Test
    void tenantContextIsClearedAfterFilter() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("42");
        when(storeRepository.existsById(42L)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        // TenantContext is cleared in the finally block
        assertThat(TenantContext.getStoreId()).isEmpty();
    }

    // ── Resolution order: header wins over subdomain ─────────────

    @Test
    void storeIdHeaderWinsOverSubdomain() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn("42");
        when(storeRepository.existsById(42L)).thenReturn(true);
        when(request.getHeader("X-Store-Slug")).thenReturn(null);
        // Subdomain would resolve differently but header wins
        when(request.getHeader("Host")).thenReturn("other-store.meethybridhub.com");
        when(request.getHeader("Authorization")).thenReturn(null);
        // Subdomain lookup would find other-store, not 42
        Store otherStore = mock(Store.class);
        when(otherStore.getId()).thenReturn(99L);
        when(storeRepository.findBySlug("other-store")).thenReturn(Optional.of(otherStore));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(42L);
    }

    @Test
    void slugHeaderWinsOverSubdomain() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/products");
        when(request.getHeader("X-Store-Id")).thenReturn(null);
        when(request.getHeader("X-Store-Slug")).thenReturn("slug-store");
        Store slugStore = mock(Store.class);
        when(slugStore.getId()).thenReturn(100L);
        when(slugStore.getStatus()).thenReturn(StoreStatus.ACTIVE);
        when(storeRepository.findBySlug("slug-store")).thenReturn(Optional.of(slugStore));
        Long[] captured = captureTenantContext();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(captured[0]).isEqualTo(100L);
    }
}
