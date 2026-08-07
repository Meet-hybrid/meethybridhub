package com.meethybridhub.store;

import com.meethybridhub.identity.validation.ValidEmail;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Write model for updating a store's branding/settings (PUT /stores/me/settings).
 *
 * Every field is optional — null/absent fields keep their current value. An
 * unknown {@code theme} value fails JSON deserialization (400); a malformed
 * color fails {@code @Pattern} (400).
 */
public record StoreSettingsUpdate(
        @Size(max = 500, message = "Logo URL must be at most 500 characters")
        String logoUrl,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be in #RRGGBB format")
        String primaryColor,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Accent color must be in #RRGGBB format")
        String accentColor,

        StoreTheme theme,

        @Size(max = 200, message = "Tagline must be at most 200 characters")
        String tagline,

        @ValidEmail
        String contactEmail
) {}
