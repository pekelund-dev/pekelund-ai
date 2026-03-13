package dev.pekelund.ai.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Incident Agent Application.
 *
 * <p>This Spring Boot application hosts three AI agents that work together to
 * manage IT incidents. It connects to the {@code incident-mcp-server} as an
 * MCP client to access operational tools.
 *
 * <h2>AI Agents</h2>
 * <ol>
 *   <li><b>TriageAgent</b> — rapidly assesses a new incident: determines severity
 *       (CRITICAL / HIGH / MEDIUM / LOW), the most likely affected service and the
 *       incident category. Uses {@code get_service_status} and {@code search_logs}.</li>
 *   <li><b>DiagnosisAgent</b> — performs a deep investigation using all MCP tools.
 *       Produces a structured root-cause analysis with supporting evidence from
 *       logs, metrics and historical incidents.</li>
 *   <li><b>IncidentOrchestrator</b> — coordinates triage and diagnosis, then
 *       synthesises a final report with an executive summary, recommended next
 *       steps, and a relevant runbook citation. Persists the report as timeline
 *       notes on the incident.</li>
 * </ol>
 *
 * <h2>MCP Client Integration</h2>
 * <p>The {@code spring-ai-starter-mcp-client} auto-configures one or more
 * {@link org.springframework.ai.mcp.client.McpSyncClient} beans based on the
 * connections defined in {@code application.yml}. A {@code ToolCallbackProvider}
 * is created from those clients; the ChatClient uses it to resolve tool calls
 * the LLM emits during inference.
 *
 * <h2>REST API</h2>
 * <ul>
 *   <li>{@code POST   /api/incidents}             — report a new incident</li>
 *   <li>{@code GET    /api/incidents}              — list all incidents</li>
 *   <li>{@code GET    /api/incidents/{id}}         — get incident details + notes</li>
 *   <li>{@code PUT    /api/incidents/{id}/status}  — update lifecycle status</li>
 *   <li>{@code POST   /api/incidents/{id}/analyse} — run full AI analysis</li>
 *   <li>{@code GET    /api/incidents/{id}/report}  — get the AI-generated report</li>
 *   <li>{@code POST   /api/demo/simulate}          — seed a demo incident and analyse it</li>
 * </ul>
 */
@SpringBootApplication
public class IncidentAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentAgentApplication.class, args);
    }
}
