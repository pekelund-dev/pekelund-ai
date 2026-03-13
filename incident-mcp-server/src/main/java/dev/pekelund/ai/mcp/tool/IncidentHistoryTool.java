package dev.pekelund.ai.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.ai.mcp.domain.Incident;
import dev.pekelund.ai.mcp.domain.IncidentNote;
import dev.pekelund.ai.mcp.repository.IncidentNoteRepository;
import dev.pekelund.ai.mcp.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP tool for incident history queries and note creation.
 *
 * <p>This tool provides two capabilities:
 * <ol>
 *   <li><b>History lookup</b> — retrieves resolved incidents similar to the current one.
 *       Past incidents include their root causes and resolutions, giving the AI agent a
 *       strong prior for diagnosing the current incident ("we've seen this before").</li>
 *   <li><b>Note creation</b> — agents can persist their analysis as timestamped notes
 *       on the incident timeline. This creates an auditable, human-readable record of
 *       the AI's reasoning that the on-call engineer can review and act on.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentHistoryTool {

    private final IncidentRepository     incidentRepository;
    private final IncidentNoteRepository noteRepository;
    private final ObjectMapper           objectMapper;

    /**
     * Retrieves resolved incidents involving a given service or category.
     *
     * @param serviceName  service name to filter by, or empty for all services
     * @param category     incident category to filter by (e.g. {@code DATABASE}, {@code NETWORK}),
     *                     or empty for all categories
     * @param limit        maximum number of past incidents to return (1–10, default 5)
     * @return JSON list of matching resolved incidents with root cause and resolution fields
     */
    @Tool(name = "get_incident_history",
          description = """
              Retrieve resolved incidents from the history database, filtered by service or category.
              Returns incident details including root cause and resolution text.
              Use this to find how similar past incidents were diagnosed and resolved —
              a strong signal for the current investigation.
              """)
    public String getIncidentHistory(
            @ToolParam(description = "Filter by service name (e.g. 'payment-service'). Leave empty for all services.")
            String serviceName,
            @ToolParam(description = "Filter by incident category (DATABASE, NETWORK, MEMORY, CPU, DEPLOYMENT, OTHER). Leave empty for all categories.")
            String category,
            @ToolParam(description = "Maximum number of past incidents to return (1–10).")
            int limit) {

        log.debug("MCP tool get_incident_history called: service={}, category={}, limit={}",
                serviceName, category, limit);

        int safeLimit = Math.min(Math.max(1, limit), 10);

        List<Incident> incidents;
        if (serviceName != null && !serviceName.isBlank()) {
            incidents = incidentRepository.findResolvedByService(serviceName, safeLimit);
        } else {
            incidents = incidentRepository.findByCategory(category, safeLimit);
        }

        return toJson(incidents.stream().map(inc -> Map.of(
                "id",              inc.getId(),
                "title",           inc.getTitle(),
                "severity",        inc.getSeverity()         != null ? inc.getSeverity()         : "UNKNOWN",
                "category",        inc.getCategory()         != null ? inc.getCategory()         : "UNKNOWN",
                "affectedServices",inc.getAffectedServices() != null ? inc.getAffectedServices() : "",
                "rootCause",       inc.getRootCause()        != null ? inc.getRootCause()        : "Not recorded",
                "resolution",      inc.getResolution()       != null ? inc.getResolution()       : "Not recorded",
                "resolvedAt",      inc.getResolvedAt()       != null ? inc.getResolvedAt().toString() : ""
        )).toList());
    }

    /**
     * Adds an AI-generated note to an incident's timeline.
     *
     * <p>Notes created by agents are marked with {@code noteType = "AUTO"} so human
     * reviewers can distinguish AI analysis from manual engineering notes.
     *
     * @param incidentId the database ID of the incident to annotate
     * @param content    the note text (markdown supported)
     * @param agentName  name of the agent creating the note (e.g. {@code TriageAgent})
     * @return JSON confirmation with the created note ID and timestamp
     */
    @Tool(name = "add_incident_note",
          description = """
              Adds an AI-generated note to an incident's timeline.
              Use this to persist analysis findings, hypotheses and recommendations as
              structured notes that human engineers can review.
              Notes are marked AUTO so they are visually distinct from manual entries.
              """)
    public String addIncidentNote(
            @ToolParam(description = "Database ID of the incident to annotate.")
            long incidentId,
            @ToolParam(description = "Content of the note (Markdown supported).")
            String content,
            @ToolParam(description = "Name of the agent creating the note, e.g. 'TriageAgent' or 'DiagnosisAgent'.")
            String agentName) {

        log.debug("MCP tool add_incident_note called: incidentId={}, agent={}", incidentId, agentName);

        Optional<Incident> incident = incidentRepository.findById(incidentId);
        if (incident.isEmpty()) {
            return "{\"error\": \"Incident " + incidentId + " not found\"}";
        }

        IncidentNote note = IncidentNote.builder()
                .incidentId(incidentId)
                .content(content)
                .noteType("AUTO")
                .author(agentName != null ? agentName : "AI Agent")
                .build();

        IncidentNote saved = noteRepository.save(note);
        log.info("Agent '{}' added note {} to incident {}", agentName, saved.getId(), incidentId);

        return toJson(Map.of(
                "success",   true,
                "noteId",    saved.getId(),
                "incidentId",incidentId,
                "author",    saved.getAuthor(),
                "createdAt", saved.getCreatedAt().toString()
        ));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise tool result", e);
            return "{\"error\": \"serialisation failure\"}";
        }
    }
}
