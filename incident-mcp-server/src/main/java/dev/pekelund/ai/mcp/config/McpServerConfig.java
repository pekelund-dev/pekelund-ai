package dev.pekelund.ai.mcp.config;

import dev.pekelund.ai.mcp.tool.IncidentHistoryTool;
import dev.pekelund.ai.mcp.tool.LogQueryTool;
import dev.pekelund.ai.mcp.tool.MetricsQueryTool;
import dev.pekelund.ai.mcp.tool.RunbookSearchTool;
import dev.pekelund.ai.mcp.tool.ServiceStatusTool;
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
 */
@Configuration
public class McpServerConfig {

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
