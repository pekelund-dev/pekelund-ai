package dev.pekelund.agent;

import dev.pekelund.agent.domain.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * AI agent responsible for rapidly triaging a new incident.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Determine incident <b>severity</b>: CRITICAL, HIGH, MEDIUM or LOW</li>
 *   <li>Identify the most likely <b>affected service(s)</b></li>
 *   <li>Classify the <b>incident category</b>: DATABASE, NETWORK, MEMORY, CPU,
 *       DEPLOYMENT or OTHER</li>
 *   <li>Suggest the <b>on-call team</b> to page</li>
 * </ul>
 *
 * <h2>MCP Tools Used</h2>
 * <ul>
 *   <li>{@code get_service_status} — lists all registered services so the agent
 *       can match service names mentioned in the incident description</li>
 *   <li>{@code search_logs} — grabs the most recent error-level log entries for
 *       the candidate service to validate the hypothesis</li>
 * </ul>
 *
 * <h2>Agent Design Pattern: Single-Turn with Tool Calls</h2>
 * <p>The triage agent runs a single ChatClient prompt turn. The LLM may emit one
 * or more tool-call requests (e.g. "give me logs for payment-service"), which the
 * Spring AI {@link ChatClient} resolves automatically by calling the MCP server,
 * injecting the results into the conversation, and continuing inference. The agent
 * then receives the final text response with severity, category and service names.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriageAgent {

    private final ChatClient chatClient;

    /**
     * System prompt that defines the triage agent's persona and output format.
     * Keeping prompts as constants makes them easy to version and review.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert Site Reliability Engineer (SRE) performing rapid incident triage.
            Your goal is to quickly classify an incident so the right team is paged.

            When given an incident title and description:
            1. Use the get_service_status tool to understand which services exist and their criticality.
            2. Use search_logs to check for recent errors in the most likely affected service.
            3. Based on the evidence, respond with a triage assessment in this EXACT format:

            SEVERITY: <CRITICAL|HIGH|MEDIUM|LOW>
            CATEGORY: <DATABASE|NETWORK|MEMORY|CPU|DEPLOYMENT|OTHER>
            AFFECTED_SERVICES: <comma-separated service names>
            TEAM: <team name responsible>
            CONFIDENCE: <HIGH|MEDIUM|LOW>
            REASONING: <2-3 sentences explaining your triage decision>

            Severity guidelines:
            - CRITICAL: Multiple services down, payment processing impacted, >50% error rate
            - HIGH: Single critical service degraded, significant user impact
            - MEDIUM: Non-critical service degraded, limited user impact
            - LOW: Minor issue, no user-facing impact

            Do not add any text outside this format. Be concise and decisive.
            """;

    /**
     * Triages an incident by analysing its description using MCP tools.
     *
     * <p>The method performs a single-turn conversation with the LLM. Under the hood,
     * the LLM will call tools (managed by the MCP client) to gather evidence before
     * producing its assessment.
     *
     * @param incident the newly created incident to triage
     * @return raw text response containing the triage assessment in the structured format
     *         defined by {@link #SYSTEM_PROMPT}
     */
    public String triage(Incident incident) {
        log.info("TriageAgent starting triage for incident {}: '{}'",
                incident.getId(), incident.getTitle());

        String userMessage = """
                Triage the following incident:

                INCIDENT ID: %d
                TITLE: %s
                DESCRIPTION: %s
                REPORTED BY: %s
                CREATED AT: %s
                """.formatted(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getReportedBy() != null ? incident.getReportedBy() : "unknown",
                incident.getCreatedAt()
        );

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        log.info("TriageAgent completed triage for incident {}", incident.getId());
        log.debug("Triage response: {}", response);

        return response;
    }
}
