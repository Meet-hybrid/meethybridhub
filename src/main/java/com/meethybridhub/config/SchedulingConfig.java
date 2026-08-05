package com.meethybridhub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} jobs (e.g. TokenCleanupService).
 *
 * Disabled via {@code app.scheduling.enabled=false} in the test profile so a
 * scheduled job can never fire mid-suite and mutate data the tests rely on.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
