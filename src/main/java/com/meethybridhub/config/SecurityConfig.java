package com.meethybridhub.config;

import com.meethybridhub.identity.JwtAuthenticationFilter;
import com.meethybridhub.store.StoreFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Phase 2 security configuration with JWT authentication.
 *
 * Updates from Phase 1:
 *   - Added JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
 *   - Configured password encoder (BCrypt)
 *   - Defined role-based access control rules
 *   - Added authentication manager bean
 *   - Protected all endpoints except explicitly public ones
 *
 * The stateless session policy remains: each request carries its own JWT,
 * no server-side session storage. This enables horizontal scaling.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final StoreFilter storeFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, StoreFilter storeFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.storeFilter = storeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Authentication endpoints (public)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/verify").permitAll()

                // User profile endpoints (any authenticated user)
                .requestMatchers("/api/v1/users/**").authenticated()

                // Admin endpoints (ADMIN role required)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // !!! INVARIANT — READ BEFORE ADDING ENDPOINTS !!!
                // Everything else is permitted at the URL level on purpose:
                //   * granular rules (roles, ownership) live in @PreAuthorize on
                //     each controller method, and
                //   * a permitAll catch-all lets unknown paths reach the 404
                //     handler instead of being masked as a 403 by the filter chain.
                // CONSEQUENCE: any new endpoint is PUBLIC unless it (a) matches a
                // URL rule above or (b) carries @PreAuthorize. Always add one of
                // the two when introducing a controller.
                .anyRequest().permitAll()
            )
            // Add JWT filter before the default username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Tenant resolution middleware: runs after authentication so the
            // store context is available to authenticated handlers.
            .addFilterAfter(storeFilter, JwtAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with default strength (10) - automatically handles salt generation
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
