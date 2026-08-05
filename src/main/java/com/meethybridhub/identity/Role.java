package com.meethybridhub.identity;

import java.util.Locale;

/**
 * The platform's roles (Hybrid's Card 3: ADMIN, STORE_OWNER, CUSTOMER).
 *
 * Roles are stored on {@link User} as a comma-separated string (Phase 2
 * decision; a role-junction table is planned for later). This enum is the
 * single source of truth for valid role names and backs Spring Security's
 * {@code ROLE_} authorities via {@link UserDetailsServiceImpl}.
 */
public enum Role {

    ADMIN,
    STORE_OWNER,
    CUSTOMER;

    /**
     * True if {@code value} names a known role (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
