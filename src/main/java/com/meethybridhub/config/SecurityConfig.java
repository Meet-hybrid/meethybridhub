package com.meethybridhub.config;

import com.meethybridhub.identity.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                
                // User endpoints (authenticated users only)
                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated()
                
                // Admin endpoints (ADMIN role required)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // Store endpoints (STORE_OWNER or ADMIN role required)
                .requestMatchers("/api/v1/stores/**").hasAnyRole("STORE_OWNER", "ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Add JWT filter before the default username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
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
