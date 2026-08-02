package com.meethybridhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The classic "does the whole context boot?" test.
 * It fails loudly if any bean, configuration, or autoconfiguration is broken —
 * a cheap daily canary for the entire application wiring.
 */
@SpringBootTest
@ActiveProfiles("test")
class MeethybridHubApplicationTests {

    @Test
    void contextLoads() {
    }
}
