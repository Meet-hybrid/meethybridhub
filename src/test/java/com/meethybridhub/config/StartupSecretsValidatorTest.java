package com.meethybridhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupSecretsValidatorTest {

    private static final String REAL_SECRET = "a-strong-random-value-32-bytes-min";

    private MockEnvironment prodEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.environment", "prod");
        env.setProperty("JWT_SECRET", REAL_SECRET);
        env.setProperty("PAYMENT_WEBHOOK_SECRET", REAL_SECRET);
        env.setProperty("DB_PASSWORD", REAL_SECRET);
        return env;
    }

    @Test
    void prodWithRealSecretsStarts() {
        assertThatCode(() -> new StartupSecretsValidator(prodEnv()).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void prodWithMissingJwtSecretRefusesToStart() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.environment", "prod");
        env.setProperty("PAYMENT_WEBHOOK_SECRET", REAL_SECRET);
        env.setProperty("DB_PASSWORD", REAL_SECRET);

        assertThatThrownBy(() -> new StartupSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void prodWithDefaultJwtSecretRefusesToStart() {
        MockEnvironment env = prodEnv();
        env.setProperty("JWT_SECRET", "changemeinproductionatleast32bytessecrethere");

        assertThatThrownBy(() -> new StartupSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("development default");
    }

    @Test
    void prodWithDefaultWebhookSecretRefusesToStart() {
        MockEnvironment env = prodEnv();
        env.setProperty("PAYMENT_WEBHOOK_SECRET", "dev-webhook-secret");

        assertThatThrownBy(() -> new StartupSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_WEBHOOK_SECRET");
    }

    @Test
    void prodWithDefaultDbPasswordRefusesToStart() {
        MockEnvironment env = prodEnv();
        env.setProperty("DB_PASSWORD", "postgres");

        assertThatThrownBy(() -> new StartupSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void prodWithAllProblemsListsThem() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.environment", "prod");

        assertThatThrownBy(() -> new StartupSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("PAYMENT_WEBHOOK_SECRET")
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void localEnvironmentSkipsValidation() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.environment", "local");

        assertThatCode(() -> new StartupSecretsValidator(env).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void missingAppEnvironmentDefaultsToLocalAndSkips() {
        MockEnvironment env = new MockEnvironment();

        assertThatCode(() -> new StartupSecretsValidator(env).validate())
                .doesNotThrowAnyException();
    }
}
