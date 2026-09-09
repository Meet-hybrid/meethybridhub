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


@Component
public class StoreFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StoreFilter.class);
    private static final String STORE_HEADER = "X-Store-Id";
    private static final String STORE_SLUG_HEADER = "X-Store-Slug";
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

        String path = request.getServletPath();
        return path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout")
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

            TenantContext.clear();
        }
    }

    private void resolveTenant(HttpServletRequest request) {
        resolveFromHeader(request)
                .or(() -> resolveFromSlugHeader(request))
                .or(() -> resolveFromSubdomain(request))
                .or(() -> resolveFromJwt(request))
                .ifPresent(storeId -> {
                    TenantContext.setStoreId(storeId);
                    log.debug("Tenant resolved to store {} for {}", storeId, request.getRequestURI());
                });
    }


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


    private Optional<Long> resolveFromSlugHeader(HttpServletRequest request) {
        String slug = request.getHeader(STORE_SLUG_HEADER);
        if (slug == null || slug.isBlank()) return Optional.empty();
        return storeRepository.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(store -> store.getStatus() == StoreStatus.ACTIVE)
                .map(Store::getId);
    }


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
        host = host.split(":")[0].toLowerCase(Locale.ROOT);
        if (host.equals(baseDomain)) {
            return Optional.empty();
        }
        if (host.endsWith("." + baseDomain)) {
            String subdomain = host.substring(0, host.length() - baseDomain.length() - 1);
            if (!subdomain.isEmpty() && !subdomain.contains(".")) {
                return Optional.of(subdomain);
            }
        }
        return Optional.empty();
    }
}
