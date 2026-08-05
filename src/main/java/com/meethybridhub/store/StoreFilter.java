package com.meethybridhub.store;

import com.meethybridhub.identity.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Tenant resolution middleware ("StoreFilter").
 *
 * Runs on every request and decides which store (tenant) the request belongs
 * to, then stores that decision in {@link TenantContext} for the rest of the
 * request. The context is always cleared afterwards, so one request can never
 * leak a tenant into another.
 *
 * Resolution order (first match wins):
 *   1. {@code X-Store-Id} header   — explicit tenant for API clients
 *   2. Subdomain from the Host header — an explicit destination (storefront),
 *      e.g. {@code divine-signature.meethybridhub.com} -> slug {@code divine-signature};
 *      wins over the claim so a multi-store owner browsing one of their stores
 *      resolves THAT store, not the one the token was issued for
 *   3. {@code storeId} JWT claim   — implicit default (the store the token was
 *      issued for at login/refresh), so dashboards need no headers or subdomain
 *
 * SECURITY NOTE: resolution alone is NOT authorization. The {@code X-Store-Id}
 * header is client-controlled, and the only thing preventing a store owner from
 * reaching another store's data is the ownership check in
 * {@code StoreService.getCurrentTenantStore} (admins bypass it). Every
 * store-scoped operation MUST funnel through that method — never read
 * TenantContext directly in a controller.
 */
@Component
public class StoreFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StoreFilter.class);
    private static final String STORE_HEADER = "X-Store-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StoreRepository storeRepository;
    private final JwtService jwtService;
    private final String baseDomain;

    public StoreFilter(StoreRepository storeRepository,
                       JwtService jwtService,
                       @Value("${store.base-domain:meethybridhub.com}") String baseDomain) {
        this.storeRepository = storeRepository;
        this.jwtService = jwtService;
        this.baseDomain = baseDomain.toLowerCase(Locale.ROOT);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // No tenant context is meaningful for public/auth infrastructure paths.
        String path = request.getServletPath();
        return path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/verify")
                || path.startsWith("/api/v1/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            resolveTenant(request);
            filterChain.doFilter(request, response);
        } finally {
            // Never leak a tenant into another request/thread.
            TenantContext.clear();
        }
    }

    private void resolveTenant(HttpServletRequest request) {
        resolveFromHeader(request)
                .or(() -> resolveFromSubdomain(request))
                .or(() -> resolveFromJwt(request))
                .ifPresent(storeId -> {
                    TenantContext.setStoreId(storeId);
                    log.debug("Tenant resolved to store {} for {}", storeId, request.getRequestURI());
                });
    }

    /**
     * 1) Explicit {@code X-Store-Id} header. The store must exist — a bogus
     * header is ignored (falls through to subdomain resolution) rather than
     * creating a phantom tenant.
     */
    private Optional<Long> resolveFromHeader(HttpServletRequest request) {
        String header = request.getHeader(STORE_HEADER);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            long storeId = Long.parseLong(header.trim());
            if (storeRepository.existsById(storeId)) {
                return Optional.of(storeId);
            }
            log.warn("X-Store-Id {} does not exist; ignoring tenant header", storeId);
        } catch (NumberFormatException e) {
            log.debug("Invalid X-Store-Id header value: {}", header);
        }
        return Optional.empty();
    }

    /**
     * 3) The {@code storeId} claim of the bearer token (signed by us at login,
     * so trustworthy). Verified to still exist before trusting it.
     */
    private Optional<Long> resolveFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Long storeId = jwtService.extractClaim(token,
                    claims -> claims.get(JwtService.CLAIM_STORE_ID, Long.class));
            if (storeId != null && storeRepository.existsById(storeId)) {
                return Optional.of(storeId);
            }
        } catch (Exception e) {
            log.debug("Could not read storeId claim: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 2) Subdomain from the Host header. Only single-label subdomains of the
     * configured base domain are considered: {@code x.meethybridhub.com} -> slug
     * {@code x}. The bare base domain and {@code localhost} carry no tenant.
     */
    private Optional<Long> resolveFromSubdomain(HttpServletRequest request) {
        return extractSubdomain(request)
                .flatMap(storeRepository::findBySlug)
                .map(Store::getId);
    }

    private Optional<String> extractSubdomain(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        host = host.split(":")[0].toLowerCase(Locale.ROOT); // strip port
        if (host.equals(baseDomain) || host.endsWith("." + baseDomain)) {
            String subdomain = host.substring(0, host.length() - baseDomain.length() - 1);
            if (!subdomain.isEmpty() && !subdomain.contains(".")) {
                return Optional.of(subdomain);
            }
        }
        return Optional.empty();
    }
}
