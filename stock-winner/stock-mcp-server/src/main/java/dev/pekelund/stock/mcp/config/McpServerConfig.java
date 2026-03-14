package dev.pekelund.stock.mcp.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import dev.pekelund.stock.mcp.tools.MarketAnalysisTools;
import dev.pekelund.stock.mcp.tools.StockDataTools;

@Configuration
public class McpServerConfig {

    /**
     * Explicit Jackson 2.x ObjectMapper bean required for MCP SDK compatibility.
     * Spring Boot 4.x uses Jackson 3.x (tools.jackson) by default, but the
     * Spring AI MCP SDK still requires Jackson 2.x (com.fasterxml.jackson).
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Bean
    public RestClient yahooFinanceRestClient() {
        return RestClient.builder()
                .baseUrl("https://query2.finance.yahoo.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; StockWinner/1.0)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Registers stock data tools with the MCP server so they can be called by the agent.
     */
    @Bean
    public ToolCallbackProvider stockDataToolCallbackProvider(StockDataTools stockDataTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(stockDataTools)
                .build();
    }

    /**
     * Registers market analysis tools with the MCP server.
     */
    @Bean
    public ToolCallbackProvider marketAnalysisToolCallbackProvider(MarketAnalysisTools marketAnalysisTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(marketAnalysisTools)
                .build();
    }
}
