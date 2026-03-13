package dev.pekelund.ai.agent;

import dev.pekelund.ai.agent.domain.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * AI agent responsible for deep root-cause analysis of an incident.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Investigate root cause using all available MCP tools</li>
 *   <li>Correlate log patterns, metric anomalies and historical incidents</li>
 *   <li>Identify the precise failure point and contributing factors</li>
 *   <li>Provide evidence-based confidence assessment</li>
 * </ul>
 *
 * <h2>MCP Tools Used</h2>
 * <ul>
 *   <li>{@code search_logs}         — finds error patterns and stack traces</li>
 *   <li>{@code get_service_metrics} — identifies metric anomalies (CPU, memory,
 *       latency, error rate)</li>
 *   <li>{@code search_runbooks}     — retrieves relevant documented procedures</li>
 *   <li>{@code get_incident_history}— finds similar past incidents and how they
 *       were resolved</li>
 *   <li>{@code get_service_status}  — understands service dependencies to assess
 *       blast radius</li>
 * </ul>
 *
 * <h2>Agent Design Pattern: Multi-Turn Investigation</h2>
 * <p>The diagnosis agent may trigger multiple rounds of tool calls as it builds
 * its analysis:
 * <ol>
 *   <li>Initial log scan → identifies error type</li>
 *   <li>Metric query → confirms resource exhaustion or spike</li>
 *   <li>Runbook search → finds documented procedure for this failure mode</li>
 *   <li>History lookup → "we've seen this before — here's how it was fixed"</li>
 * </ol>
 * <p>Spring AI's {@link ChatClient} handles this automatically: each tool call
 * result is appended to the conversation context and inference continues until
 * the LLM emits a final text response (no more tool calls).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosisAgent {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a senior infrastructure engineer performing in-depth incident diagnosis.
            Your job is to determine the root cause of an incident using all available tools.

            Investigation strategy:
            1. Use search_logs to find error patterns in the affected service(s)
            2. Use get_service_metrics to check for resource exhaustion (CPU, memory, error rate, latency)
            3. Use get_incident_history to find similar past incidents and their resolutions
            4. Use search_runbooks to find documented procedures for this type of failure
            5. Use get_service_status to understand service dependencies and blast radius
            6. Use add_incident_note to persist your key findings as you go

            Produce a structured diagnosis in this EXACT format:

            ROOT_CAUSE: <one-sentence root cause statement>
            CONTRIBUTING_FACTORS: <numbered list of contributing factors>
            EVIDENCE:
              - LOGS: <key log evidence>
              - METRICS: <key metric evidence>
              - HISTORY: <relevant past incidents>
            RECOMMENDED_RUNBOOK: <runbook title if found, otherwise NONE>
            BLAST_RADIUS: <which services are affected and how>
            CONFIDENCE: <HIGH|MEDIUM|LOW>
            REASONING: <3-5 sentences of analytical reasoning>

            Be thorough but concise. Use facts from tools, not generic advice.
            """;

    /**
     * Performs a deep root-cause analysis of an incident.
     *
     * <p>This method may take longer than triage as the LLM is instructed to
     * query multiple tools and build a comprehensive analysis. The tool-call
     * loop is managed transparently by the {@link ChatClient}.
     *
     * @param incident the incident to diagnose (should already have a severity and
     *                 affected services set by the TriageAgent)
     * @return raw text response containing the structured diagnosis
     */
    public String diagnose(Incident incident) {
        log.info("DiagnosisAgent starting investigation for incident {}: '{}'",
                incident.getId(), incident.getTitle());

        String userMessage = """
                Perform a thorough root-cause analysis for this incident:

                INCIDENT ID: %d
                TITLE: %s
                DESCRIPTION: %s
                SEVERITY: %s
                AFFECTED_SERVICES: %s
                CATEGORY: %s
                CREATED AT: %s

                Use all available tools to investigate. Persist key findings as incident notes.
                """.formatted(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity()         != null ? incident.getSeverity()         : "UNKNOWN",
                incident.getAffectedServices() != null ? incident.getAffectedServices() : "UNKNOWN",
                incident.getCategory()         != null ? incident.getCategory()         : "UNKNOWN",
                incident.getCreatedAt()
        );

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        log.info("DiagnosisAgent completed investigation for incident {}", incident.getId());
        log.debug("Diagnosis response: {}", response);

        return response;
    }
}
