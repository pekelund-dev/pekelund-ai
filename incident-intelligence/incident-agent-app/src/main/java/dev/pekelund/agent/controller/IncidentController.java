package dev.pekelund.agent.controller;

import dev.pekelund.agent.domain.Incident;
import dev.pekelund.agent.domain.IncidentNote;
import dev.pekelund.agent.dto.AnalysisResult;
import dev.pekelund.agent.dto.CreateIncidentRequest;
import dev.pekelund.agent.dto.IncidentResponse;
import dev.pekelund.agent.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST controller for the incident management API.
 *
 * <p>Base path: {@code /api/incidents}
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/api/incidents</td><td>Report a new incident</td></tr>
 *   <tr><td>GET</td><td>/api/incidents</td><td>List all incidents</td></tr>
 *   <tr><td>GET</td><td>/api/incidents/{id}</td><td>Get incident details + notes</td></tr>
 *   <tr><td>PUT</td><td>/api/incidents/{id}/status</td><td>Update lifecycle status</td></tr>
 *   <tr><td>POST</td><td>/api/incidents/{id}/notes</td><td>Add a manual note</td></tr>
 *   <tr><td>POST</td><td>/api/incidents/{id}/analyse</td><td>Trigger full AI analysis</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * Reports a new incident.
     *
     * @param request the incident details (title, description, reportedBy)
     * @return 201 Created with the incident ID and initial status
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {

        Incident incident = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id",        incident.getId(),
                "title",     incident.getTitle(),
                "status",    incident.getStatus(),
                "message",   "Incident reported. Call POST /api/incidents/" + incident.getId() + "/analyse to trigger AI analysis."
        ));
    }

    /**
     * Lists all incidents, newest first.
     */
    @GetMapping
    public List<IncidentResponse> listIncidents() {
        return incidentService.getAllIncidents().stream()
                .map(inc -> IncidentResponse.from(inc, incidentService.getNotes(inc.getId())))
                .toList();
    }

    /**
     * Returns full details of a single incident, including its timeline notes.
     */
    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable Long id) {
        Incident incident = incidentService.getIncident(id);
        List<IncidentNote> notes = incidentService.getNotes(id);
        return IncidentResponse.from(incident, notes);
    }

    /**
     * Updates the lifecycle status of an incident.
     *
     * @param id     incident ID
     * @param body   JSON body: {@code {"status": "RESOLVED"}}
     */
    @PutMapping("/{id}/status")
    public IncidentResponse updateStatus(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Request body must contain a 'status' field");
        }
        Incident updated = incidentService.updateStatus(id, newStatus);
        return IncidentResponse.from(updated, incidentService.getNotes(id));
    }

    /**
     * Adds a manual note to an incident's timeline.
     *
     * @param id   incident ID
     * @param body JSON body: {@code {"content": "...", "author": "..."}}
     */
    @PostMapping("/{id}/notes")
    public ResponseEntity<IncidentResponse.NoteResponse> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String content = body.get("content");
        String author  = body.getOrDefault("author", "unknown");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Request body must contain a 'content' field");
        }

        IncidentNote note = incidentService.addManualNote(id, content, author);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(IncidentResponse.NoteResponse.from(note));
    }

    /**
     * Triggers the full AI analysis pipeline for an incident.
     *
     * <p><b>Note:</b> This is a synchronous long-running operation. Expect 30–120 seconds
     * for the LLM to complete triage, diagnosis and summary. In production this would be
     * made asynchronous with a job-status endpoint.
     *
     * @param id incident ID
     * @return the complete AI analysis result
     */
    @PostMapping("/{id}/analyse")
    public AnalysisResult analyseIncident(@PathVariable Long id) {
        log.info("REST request: analyse incident {}", id);
        return incidentService.analyse(id);
    }

    // ── Exception Handlers ────────────────────────────────────────────────────

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
