package com.meethybridhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration for automatic timestamp management.
 *
 * Enables automatic population of:
 *   - @CreatedDate - when entity is created
 *   - @LastModifiedDate - when entity is updated
 *   - @CreatedBy - who created the entity
 *   - @LastModifiedBy - who last modified the entity
 *
 * For Phase 2, we're only using timestamp auditing (CreatedDate, LastModifiedDate).
 * User auditing (CreatedBy, LastModifiedBy) will be added when we have user context.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * AuditorAware implementation for Spring Security integration.
     *
     * Returns the current authenticated user's username (email).
     * Returns "system" when no authentication is present (e.g., system processes).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return Optional.of(((org.springframework.security.core.userdetails.UserDetails) principal).getUsername());
            }
            
            return Optional.of(principal.toString());
        };
    }
}