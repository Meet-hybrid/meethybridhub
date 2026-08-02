package com.meethybridhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the MeethybridHub API.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration        (this class is a source of bean definitions)
 *   - @EnableAutoConfiguration (Spring Boot wires the environment: web server,
 *                               JPA, Security, Jackson, etc. based on classpath)
 *   - @ComponentScan        (finds @Component/@Service/@Repository/@RestController
 *                               classes in this package and below)
 *
 * Component scanning means we DON'T hand-register every class in Spring's
 * application context — that is the foundation of Spring Boot's "convention
 * over configuration" model.
 */
@SpringBootApplication
public class MeethybridHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeethybridHubApplication.class, args);
    }
}
