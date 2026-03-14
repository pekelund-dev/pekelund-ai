package dev.pekelund.ai.agent.config;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Spring AI A2A protocol configuration.
 *
 * <h2>What is A2A?</h2>
 * <p>A2A (Agent-to-Agent) is an open standard by Google (2025) that enables AI agents
 * to discover and call <em>each other</em> using plain HTTP + JSON. Any A2A-compatible
 * orchestrator, client library, or agent can interact with this service without knowing
 * its internal implementation.
 *
 * <h2>Framework Integration</h2>
 * <p>This class provides the two user-defined beans required by the
 * {@code spring-ai-a2a-server-autoconfigure} library:
 * <ol>
 *   <li>{@link AgentCard} — the agent's self-describing discovery document</li>
 *   <li>{@link AgentExecutor} — the logic that processes incoming A2A tasks</li>
 * </ol>
 *
 * <p>Once these beans are present the autoconfigure library automatically exposes:
 * <ul>
 *   <li>{@code GET  /.well-known/agent-card.json} — agent card discovery endpoint</li>
 *   <li>{@code GET  /card}                        — alternative agent card endpoint</li>
 *   <li>{@code POST /}                            — JSON-RPC message / task send</li>
 *   <li>{@code GET  /tasks/{id}}                  — task status retrieval</li>
 *   <li>{@code POST /tasks/{id}/cancel}           — task cancellation</li>
 * </ul>
 *
 * <p>All endpoints are relative to {@code server.servlet.context-path=/a2a}, so the
 * full external URLs are {@code /a2a}, {@code /a2a/.well-known/agent-card.json}, etc.
 *
 * <h2>Skill Routing</h2>
 * <p>Clients can pass an optional {@code skillId} in the request {@code metadata} map:
 * <ul>
 *   <li>{@code incident_analysis} — full 3-phase pipeline with DB persistence (default)</li>
 *   <li>{@code incident_triage}   — fast triage only, no persistence</li>
 * </ul>
 */
@Configuration
public class A2AConfig {

    /** Agent card task URL — relative to the servlet context path ({@code /a2a}). */
    private static final String TASK_URL_PATH = "/";

    /**
     * System prompt for full incident analysis (triage + diagnosis + executive summary).
     *
     * <p>The ChatClient has all MCP tools wired ({@code get_service_status},
     * {@code search_logs}, {@code get_service_metrics}, {@code search_runbooks},
     * {@code get_incident_history}, {@code add_incident_note}).
     * The LLM calls them automatically to gather evidence before responding.
     */
    private static final String ANALYSIS_SYSTEM_PROMPT = """
            You are an expert IT Incident Response engineer performing a full incident analysis.
            You have access to MCP tools for querying logs, metrics, runbooks, service status and incident history.

            When given an incident title and description:
            1. Call get_service_status to understand which services exist and their criticality
            2. Call search_logs to look for recent errors in the affected services
            3. Call get_service_metrics to check for resource anomalies
            4. Call search_runbooks to find relevant remediation procedures
            5. Call get_incident_history to check for precedents
            6. Synthesize all evidence into a comprehensive incident analysis report

            Your report MUST follow this structure:
            ## Incident Analysis Report

            **SEVERITY**: <CRITICAL|HIGH|MEDIUM|LOW>
            **CATEGORY**: <DATABASE|NETWORK|MEMORY|CPU|DEPLOYMENT|OTHER>
            **AFFECTED SERVICES**: <comma-separated list>
            **CONFIDENCE**: <HIGH|MEDIUM|LOW>

            ### Root Cause
            <2-3 sentences describing the most likely root cause based on evidence>

            ### Executive Summary
            <3-5 sentences suitable for stakeholders, non-technical>

            ### Recommended Steps
            <numbered list of immediate remediation steps>

            ### Relevant Runbook
            <runbook reference if found, or "No specific runbook found">

            Be specific, cite evidence from tool results, and be decisive.
            """;

    /**
     * System prompt for fast triage only — used when client passes {@code skillId: incident_triage}.
     */
    private static final String TRIAGE_SYSTEM_PROMPT = """
            You are an expert Site Reliability Engineer performing rapid incident triage.
            You have access to MCP tools. Use get_service_status and search_logs to gather evidence quickly.

            Respond with a triage assessment in this EXACT format:
            SEVERITY: <CRITICAL|HIGH|MEDIUM|LOW>
            CATEGORY: <DATABASE|NETWORK|MEMORY|CPU|DEPLOYMENT|OTHER>
            AFFECTED_SERVICES: <comma-separated service names>
            TEAM: <responsible team name>
            CONFIDENCE: <HIGH|MEDIUM|LOW>
            REASONING: <2-3 sentences explaining your triage decision>

            Do not add any text outside this format. Be concise and decisive.
            """;

    /**
     * Builds and returns the A2A Agent Card for this service.
     *
     * <p>The agent card is served by the auto-configured
     * {@code AgentCardController} at {@code GET /.well-known/agent-card.json}
     * and {@code GET /card} (both relative to the servlet context path).
     *
     * @param port the server port, used to build the absolute task URL
     * @return the A2A agent card describing this service
     */
    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port) {
        String taskUrl = "http://localhost:" + port + "/a2a/";
        return new AgentCard.Builder()
                .name("ITII Incident Analysis Agent")
                .description("AI-powered IT incident triage and root-cause analysis. " +
                        "Provide an incident title and description and the agent will use " +
                        "MCP tools (logs, metrics, runbooks, incident history) to diagnose " +
                        "the root cause, assess severity and recommend remediation steps.")
                .url(taskUrl)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("incident_analysis")
                                .name("Full Incident Analysis")
                                .description("Runs the full 3-phase AI investigation pipeline: " +
                                        "service status check, log analysis, metrics review, " +
                                        "runbook lookup, incident history comparison, and " +
                                        "executive summary generation. Pass skillId: incident_analysis " +
                                        "in request metadata (or omit for default).")
                                .tags(List.of("incident", "triage", "diagnosis", "root-cause", "sre"))
                                .examples(List.of(
                                        "title: Payment service returning 503\n" +
                                        "description: Error rate at 80%, started 5 minutes ago",
                                        "title: Fraud service OOM crash\n" +
                                        "description: Pod restarting every 90 minutes since v2.4.1 deployment"
                                ))
                                .inputModes(List.of("text"))
                                .outputModes(List.of("text"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("incident_triage")
                                .name("Fast Incident Triage")
                                .description("Rapid triage using get_service_status and search_logs. " +
                                        "Returns severity, category, affected services and responsible team. " +
                                        "No DB persistence. Pass skillId: incident_triage in request metadata.")
                                .tags(List.of("incident", "triage", "sre"))
                                .examples(List.of(
                                        "title: API gateway 503\ndescription: All endpoints returning 503 since 15 minutes"
                                ))
                                .inputModes(List.of("text"))
                                .outputModes(List.of("text"))
                                .build()
                ))
                .protocolVersion("0.3.0")
                .build();
    }

    /**
     * Builds and returns the A2A AgentExecutor that processes incoming tasks.
     *
     * <p>Uses Spring AI's {@link DefaultAgentExecutor} which handles the full A2A
     * task lifecycle (submit → working → completed/failed) automatically.
     * The {@link ChatClient} already has all MCP tool callbacks wired, so the
     * LLM can call them during the response generation.
     *
     * <h3>Skill Routing</h3>
     * <p>The executor checks for a {@code skillId} key in the request metadata:
     * <ul>
     *   <li>{@code incident_triage}   — fast triage with minimal tools</li>
     *   <li>{@code incident_analysis} — full analysis pipeline (default)</li>
     * </ul>
     *
     * @param chatClient the pre-configured ChatClient with MCP tool callbacks
     * @return the AgentExecutor wired to the incident analysis system prompt
     */
    @Bean
    public AgentExecutor agentExecutor(ChatClient chatClient) {
        return new DefaultAgentExecutor(chatClient, (chat, ctx) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(ctx.getMessage());

            // Check for skill preference in request metadata
            Map<String, Object> metadata = ctx.getParams() != null
                    ? ctx.getParams().metadata()
                    : null;
            String skillId = metadata != null ? (String) metadata.get("skillId") : null;

            String systemPrompt = "incident_triage".equals(skillId)
                    ? TRIAGE_SYSTEM_PROMPT
                    : ANALYSIS_SYSTEM_PROMPT;

            return chat.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        });
    }
}
