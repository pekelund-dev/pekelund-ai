package dev.pekelund.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.pekelund.mcp.tool.IncidentHistoryTool;
import dev.pekelund.mcp.tool.LogQueryTool;
import dev.pekelund.mcp.tool.MetricsQueryTool;
import dev.pekelund.mcp.tool.RunbookSearchTool;
import dev.pekelund.mcp.tool.ServiceStatusTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration.
 *
 * <p>This class registers all tool implementation beans as a {@link ToolCallbackProvider}.
 * The Spring AI MCP Server auto-configuration picks up this provider and advertises
 * every tool over the MCP protocol, making them discoverable and invocable by any
 * MCP-compatible client.
 *
 * <h2>How Spring AI MCP Tool Registration Works</h2>
 * <ol>
 *   <li>Methods annotated with {@code @Tool} on Spring beans define the tool contract
 *       (name, description, and parameter schema).</li>
 *   <li>{@link MethodToolCallbackProvider} wraps those beans and produces one
 *       {@code ToolCallback} per {@code @Tool}-annotated method.</li>
 *   <li>The MCP Server starter discovers all {@link ToolCallbackProvider} beans and
 *       registers the callbacks as MCP tools, exposed via HTTP/SSE transport.</li>
 * </ol>
 *
 * <h2>Jackson 2.x ObjectMapper Bean</h2>
 * <p>Spring Boot 4.x auto-configures a Jackson 3.x {@code ObjectMapper}
 * ({@code tools.jackson.databind.ObjectMapper}). Our tool implementations use
 * Jackson 2.x ({@code com.fasterxml.jackson.databind.ObjectMapper}) which is
 * provided by the Spring AI MCP SDK. We register an explicit
 * {@code com.fasterxml.jackson.databind.ObjectMapper} bean here so the tools
 * can inject it without relying on Spring Boot's auto-configuration.
 */
@Configuration
public class McpServerConfig {

    /**
     * Explicit Jackson 2.x {@code ObjectMapper} bean used by all MCP tool implementations.
     *
     * <p>Spring Boot 4.x moved to Jackson 3.x ({@code tools.jackson.*} namespace).
     * The Spring AI MCP SDK still uses Jackson 2.x ({@code com.fasterxml.jackson.*}),
     * so we register this explicit bean to satisfy the tools' constructor injection.
     * There is no conflict with Spring Boot's auto-configured Jackson 3.x ObjectMapper
     * because the two classes are in different namespaces and packages.
     */
    @Bean
    public ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    /**
     * Registers all operational tool implementations with the MCP server.
     *
     * <p>Adding or removing tool objects here is the only change required to
     * expose new capabilities to AI agents — no protocol plumbing needed.
     */
    @Bean
    public ToolCallbackProvider incidentToolCallbackProvider(
            LogQueryTool logQueryTool,
            MetricsQueryTool metricsQueryTool,
            RunbookSearchTool runbookSearchTool,
            IncidentHistoryTool incidentHistoryTool,
            ServiceStatusTool serviceStatusTool) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(logQueryTool, metricsQueryTool, runbookSearchTool,
                        incidentHistoryTool, serviceStatusTool)
                .build();
    }
}
