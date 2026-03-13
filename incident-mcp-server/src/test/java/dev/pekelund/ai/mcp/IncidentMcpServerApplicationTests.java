package dev.pekelund.ai.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the application context loads without errors.
 *
 * <p>Uses the {@code test} profile which switches to an in-memory H2 database
 * so no external PostgreSQL is needed during CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class IncidentMcpServerApplicationTests {

    @Test
    void contextLoads() {
        // If the application context starts without throwing, the test passes.
        // This catches mis-configured beans, missing properties, etc.
    }
}
