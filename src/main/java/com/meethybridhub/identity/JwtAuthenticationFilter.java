package com.meethybridhub.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that runs once per request.
 *
 * This filter:
 * 1. Extracts the JWT from the Authorization header (Bearer token)
 * 2. Validates the token using JwtService
 * 3. Loads user details from UserDetailsService
 * 4. Sets the authentication in SecurityContext
 *
 * Stateless by design: no session is created, authentication is validated
 * on every request. This enables horizontal scaling (any instance can serve
 * any request).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Skip filter for public endpoints (already configured in SecurityConfig)
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // No Authorization header → continue as unauthenticated
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String userEmail = jwtService.extractUsername(jwt);

            // If we have a username and no existing authentication in context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                
                // Validate the token AND that the account may actually use the API.
                // The password-version check rejects every token issued before a
                // password reset/change.
                if (jwtService.validateToken(jwt, userDetails)
                        && jwtService.passwordVersionMatches(jwt, userDetails)
                        && userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()) {
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials are null since we're using token auth
                            userDetails.getAuthorities()
                    );
                    
                    // Add request details (IP, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Authenticated user: {}", userEmail);
                } else {
                    log.debug("Token rejected for user {} (invalid, stale password version, unverified, or locked)", userEmail);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the request - let authorization handle it
            log.error("JWT authentication error for request {}: {}", 
                     request.getRequestURI(), e.getMessage(), e);
            
            // Clear security context in case of any contamination
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if request is for a public endpoint.
     * These endpoints are configured in SecurityConfig and don't require authentication.
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        
        // Public endpoints from SecurityConfig
        return path.startsWith("/actuator/health") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/swagger-ui.html") ||
               path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/register") ||
               path.equals("/api/v1/auth/verify") ||
               path.equals("/api/v1/auth/refresh") ||
               path.equals("/api/v1/auth/reset-password") ||
               path.equals("/api/v1/auth/resend-verification");
    }

    /**
     * Should not filter error dispatches.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Should not filter async dispatches.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }
}