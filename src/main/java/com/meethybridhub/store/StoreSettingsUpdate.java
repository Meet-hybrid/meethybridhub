package com.meethybridhub.store;

import com.meethybridhub.identity.validation.ValidEmail;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


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
