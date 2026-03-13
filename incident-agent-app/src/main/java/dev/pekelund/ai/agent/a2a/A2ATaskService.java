package dev.pekelund.ai.agent.a2a;

import dev.pekelund.ai.agent.TriageAgent;
import dev.pekelund.ai.agent.domain.Incident;
import dev.pekelund.ai.agent.dto.AnalysisResult;
import dev.pekelund.ai.agent.dto.CreateIncidentRequest;
import dev.pekelund.ai.agent.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes A2A tasks by routing them to the appropriate AI agent.
 *
 * <h2>Skill Routing</h2>
 * <table border="1">
 *   <tr><th>skillId</th><th>Agent Used</th><th>Description</th></tr>
 *   <tr>
 *     <td>{@code incident_triage}</td>
 *     <td>{@link TriageAgent}</td>
 *     <td>Fast triage: severity, category and affected services. No DB persistence.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code incident_analysis} (default)</td>
 *     <td>{@link dev.pekelund.ai.agent.IncidentOrchestrator}</td>
 *     <td>Full pipeline: creates an incident, runs triage + diagnosis + summary,
 *         persists results.</td>
 *   </tr>
 * </table>
 *
 * <h2>Task Storage</h2>
 * <p>Completed tasks are cached in a bounded in-memory map so they can be retrieved
 * via {@code tasks/get} for a short period after completion. This is sufficient for
 * demo purposes; a production system would use a persistent task store.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class A2ATaskService {

    /** Skill ID for full analysis (triage + diagnosis + executive summary). */
    public static final String SKILL_FULL_ANALYSIS = "incident_analysis";
    /** Skill ID for fast triage only (no persistence). */
    public static final String SKILL_TRIAGE_ONLY   = "incident_triage";

    private final IncidentService incidentService;
    private final TriageAgent     triageAgent;

    /**
     * In-memory task cache.
     * Key: task ID supplied by the A2A client.
     * Value: completed or failed {@link A2ATask}.
     */
    private final ConcurrentHashMap<String, A2ATask> taskCache = new ConcurrentHashMap<>();

    /**
     * Processes a new A2A task synchronously and caches the result.
     *
     * @param taskId   client-supplied task ID (UUID)
     * @param message  the user's input message
     * @param skillId  which skill to invoke; defaults to {@link #SKILL_FULL_ANALYSIS}
     * @return the completed (or failed) {@link A2ATask}
     */
    public A2ATask processTask(String taskId, A2AMessage message, String skillId) {
        String skill = (skillId != null && !skillId.isBlank()) ? skillId : SKILL_FULL_ANALYSIS;
        log.info("A2A task {} — processing with skill '{}'", taskId, skill);

        A2ATask result;
        try {
            result = switch (skill) {
                case SKILL_TRIAGE_ONLY -> runTriageOnly(taskId, message);
                default                -> runFullAnalysis(taskId, message);
            };
        } catch (Exception e) {
            log.error("A2A task {} failed: {}", taskId, e.getMessage(), e);
            result = new A2ATask(taskId, A2ATaskStatus.failed("Task failed: " + e.getMessage()), null);
        }

        taskCache.put(taskId, result);
        return result;
    }

    /**
     * Retrieves a previously completed task from the in-memory cache.
     *
     * @param taskId the task ID
     * @return the cached task, or empty if not found
     */
    public Optional<A2ATask> getTask(String taskId) {
        return Optional.ofNullable(taskCache.get(taskId));
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    /**
     * Runs the full 3-phase analysis pipeline (triage + diagnosis + summary).
     * Creates a persistent incident record in the database.
     */
    private A2ATask runFullAnalysis(String taskId, A2AMessage userMessage) {
        String text  = extractText(userMessage);
        String title = extractTitle(text);
        String desc  = extractDescription(text);

        // Create a persistent incident so MCP tools (add_incident_note) can annotate it
        Incident incident = incidentService.createIncident(
                new CreateIncidentRequest(title, desc, "a2a-client"));

        log.info("A2A task {} → created incident {}", taskId, incident.getId());

        AnalysisResult analysis = incidentService.analyse(incident.getId());

        String report = buildFullReport(analysis);
        A2AMessage responseMessage = new A2AMessage("agent", List.of(A2AMessage.A2APart.text(report)));

        return new A2ATask(
                taskId,
                A2ATaskStatus.completed(responseMessage),
                Map.of("incidentId", incident.getId(), "severity", analysis.severity())
        );
    }

    /**
     * Runs fast triage only — no database persistence.
     * The incident is created temporarily in memory for the TriageAgent to evaluate.
     */
    private A2ATask runTriageOnly(String taskId, A2AMessage userMessage) {
        String text  = extractText(userMessage);
        String title = extractTitle(text);
        String desc  = extractDescription(text);

        // Create a transient incident object (not persisted) for the triage agent
        Incident transient_ = Incident.builder()
                .id(0L)                  // sentinel — not saved to DB
                .title(title)
                .description(desc)
                .reportedBy("a2a-client")
                .build();

        String triageResult = triageAgent.triage(transient_);

        A2AMessage responseMessage = new A2AMessage("agent",
                List.of(A2AMessage.A2APart.text(triageResult)));

        return new A2ATask(taskId, A2ATaskStatus.completed(responseMessage), null);
    }

    /** Extracts the first {@code text/plain} part from a message. */
    private String extractText(A2AMessage message) {
        if (message == null || message.parts() == null) {
            return "";
        }
        return message.parts().stream()
                .filter(p -> "text/plain".equals(p.type()) && p.text() != null)
                .map(A2AMessage.A2APart::text)
                .findFirst()
                .orElse("");
    }

    /**
     * Extracts the incident title from the message text.
     *
     * <p>Clients can use the format {@code title: <title>\ndescription: <description>}
     * for structured input. If no explicit title is found, the first line is used as the title.
     */
    private String extractTitle(String text) {
        for (String line : text.split("\n")) {
            String lc = line.toLowerCase().trim();
            if (lc.startsWith("title:")) {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        // Fall back to first non-empty line, truncated to 255 chars
        String firstLine = text.lines().filter(l -> !l.isBlank()).findFirst().orElse("A2A Incident");
        return firstLine.length() > 255 ? firstLine.substring(0, 255) : firstLine;
    }

    /**
     * Extracts the incident description from the message text.
     *
     * <p>Looks for an explicit {@code description:} prefix; otherwise uses the full message.
     */
    private String extractDescription(String text) {
        boolean capturing = false;
        StringBuilder desc = new StringBuilder();
        for (String line : text.split("\n")) {
            String lc = line.toLowerCase().trim();
            if (lc.startsWith("description:")) {
                capturing = true;
                String rest = line.substring(line.indexOf(':') + 1).trim();
                if (!rest.isBlank()) desc.append(rest);
            } else if (capturing) {
                desc.append("\n").append(line);
            }
        }
        if (!desc.isEmpty()) {
            return desc.toString().trim();
        }
        return text; // return full text if no explicit description section
    }

    /** Formats an {@link AnalysisResult} into a readable A2A response text. */
    private String buildFullReport(AnalysisResult analysis) {
        return """
                ## Incident Analysis Report

                **SEVERITY**: %s
                **CATEGORY**: %s
                **AFFECTED SERVICES**: %s
                **CONFIDENCE**: %s

                ### Root Cause
                %s

                ### Executive Summary
                %s

                ### Recommended Steps
                %s

                %s
                """.formatted(
                analysis.severity(),
                analysis.category(),
                analysis.affectedServices(),
                analysis.confidence(),
                analysis.rootCause(),
                analysis.executiveSummary(),
                analysis.recommendedSteps(),
                analysis.runbookReference() != null
                        ? "### Relevant Runbook\n" + analysis.runbookReference()
                        : ""
        );
    }
}
