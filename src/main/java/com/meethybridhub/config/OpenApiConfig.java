package com.meethybridhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) metadata for the API.
 *
 * springdoc generates a live, interactive spec at /swagger-ui.html by scanning
 * controllers + their annotations. Declaring the JWT bearer scheme here means
 * every secured endpoint can be tested from the UI once auth exists (Phase 2).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meethybridHubOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MeethybridHub API")
                .description("""
                    Multi-tenant e-commerce SaaS platform.

                    Every store is a tenant with its own branded storefront and
                    subdomain (e.g. divinesignature.meethybridhub.com). A single
                    customer account spans all stores. Installment payments are a
                    first-class feature.
                    """)
                .version("v0.1.0")
                .contact(new Contact().name("Meethybrid Engineering")))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
