package dev.pekelund.stock.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.ai.google.genai.api-key=test-key",
                "spring.ai.mcp.client.enabled=false",
                "spring.ai.a2a.server.enabled=false"
        })
class StockAgentApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ToolCallbackProvider toolCallbackProvider() {
            return () -> new ToolCallback[0];
        }
    }

    @Test
    void contextLoads() {
    }
}
