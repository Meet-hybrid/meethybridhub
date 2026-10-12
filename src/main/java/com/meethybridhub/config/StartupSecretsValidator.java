package com.meethybridhub.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start the application in production when critical secrets are
 * missing or still set to their committed local defaults. Without this check a
 * prod deploy with an unset env var silently boots with dev credentials
 * (predictable JWT signing key, known webhook secret, default DB password).
 *
 * Activated by APP_ENV=prod; local/test environments are unaffected.
 */
@Component
public class StartupSecretsValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupSecretsValidator.class);

    private final Environment environment;

    public StartupSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String env = environment.getProperty("app.environment", "local");
        if (!"prod".equalsIgnoreCase(env)) {
            log.info("Startup secrets validation skipped for environment '{}'", env);
            return;
        }

        List<String> problems = new ArrayList<>();
        requireExternalValue("JWT_SECRET",
                "changemeinproductionatleast32bytessecrethere", problems);
        requireExternalValue("PAYMENT_WEBHOOK_SECRET",
                "dev-webhook-secret", problems);
        requireExternalValue("DB_PASSWORD",
                "postgres", problems);

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with APP_ENV=prod: the following secrets are missing, blank, "
                    + "or still set to their local development defaults: " + String.join(", ", problems)
                    + ". Provide them via environment variables or a secret manager.");
        }
        log.info("Startup secrets validation passed for production environment");
    }

    private void requireExternalValue(String name, String forbiddenDefault, List<String> problems) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            problems.add(name + " (not set)");
        } else if (value.equals(forbiddenDefault)) {
            problems.add(name + " (still the development default)");
        }
    }
}
