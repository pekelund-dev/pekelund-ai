package dev.pekelund.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test verifying the agent application context loads correctly.
 *
 * <p>Uses the {@code test} profile which disables the MCP client connection
 * and uses an in-memory H2 database, so no running MCP server or OpenAI
 * API key is required.
 */
@SpringBootTest
@ActiveProfiles("test")
class IncidentAgentApplicationTests {

    @Test
    void contextLoads() {
        // Context load success = test passes
    }
}
