package dev.pekelund.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Incident MCP Server.
 *
 * <p>This Spring Boot application acts as the <b>MCP (Model Context Protocol) Server</b>
 * in the IT Incident Intelligence Platform. It exposes <b>six</b> operational tools via
 * HTTP/SSE transport (default endpoint: {@code /sse}):
 *
 * <ol>
 *   <li>{@code search_logs}          — full-text search over simulated application logs</li>
 *   <li>{@code get_service_metrics}  — retrieve CPU, memory, error-rate and latency metrics</li>
 *   <li>{@code search_runbooks}      — keyword/service search over the runbook knowledge base</li>
 *   <li>{@code get_incident_history} — find similar past incidents by service or category</li>
 *   <li>{@code add_incident_note}    — persist AI analysis as timestamped timeline notes</li>
 *   <li>{@code get_service_status}   — current health and metadata for a given service</li>
 * </ol>
 *
 * <p>Any MCP-compatible client (including the {@code incident-agent-app} module) can
 * connect to this server, list available tools, and invoke them. The AI agents use these
 * tools to gather context before forming their recommendations.
 *
 * <h2>MCP Primer</h2>
 * <p>The Model Context Protocol defines a standard way for LLM-based applications to
 * discover and invoke external tools. The server advertises a tool catalogue; clients
 * call tools by name, passing structured arguments; the server returns structured results.
 * This decoupling means the same tool server can be reused by many different agents.
 */
@SpringBootApplication
public class IncidentMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentMcpServerApplication.class, args);
    }
}
