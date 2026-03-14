package dev.pekelund.agent.service;

import dev.pekelund.agent.IncidentOrchestrator;
import dev.pekelund.agent.domain.Incident;
import dev.pekelund.agent.domain.IncidentNote;
import dev.pekelund.agent.dto.AnalysisResult;
import dev.pekelund.agent.dto.CreateIncidentRequest;
import dev.pekelund.agent.repository.IncidentNoteRepository;
import dev.pekelund.agent.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Application service layer for incident management.
 *
 * <p>Encapsulates all business logic for incidents: creation, status updates,
 * manual note addition and triggering AI analysis. Controllers remain thin
 * by delegating to this service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository     incidentRepository;
    private final IncidentNoteRepository noteRepository;
    private final IncidentOrchestrator   orchestrator;

    /**
     * Creates a new incident from the request, persists it, and returns the saved entity.
     *
     * <p>Note: AI analysis is <em>not</em> triggered automatically on creation — the caller
     * (controller or demo endpoint) decides when to kick off the analysis. This avoids
     * unexpected latency on the creation endpoint.
     */
    @Transactional
    public Incident createIncident(CreateIncidentRequest request) {
        Incident incident = Incident.builder()
                .title(request.title())
                .description(request.description())
                .reportedBy(request.reportedBy())
                .status("OPEN")
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Created incident {} — '{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Returns all incidents ordered by creation time (newest first).
     */
    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Returns a single incident by ID.
     *
     * @throws NoSuchElementException if no incident with that ID exists
     */
    @Transactional(readOnly = true)
    public Incident getIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
    }

    /**
     * Returns all timeline notes for an incident (most recent first).
     */
    @Transactional(readOnly = true)
    public List<IncidentNote> getNotes(Long incidentId) {
        return noteRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    /**
     * Updates the lifecycle status of an incident.
     *
     * <p>When transitioning to {@code RESOLVED}, the {@code resolvedAt} timestamp is set.
     *
     * @param id        incident ID
     * @param newStatus new status string (OPEN / IN_PROGRESS / RESOLVED / ESCALATED)
     * @return the updated incident
     * @throws NoSuchElementException if no incident with that ID exists
     */
    @Transactional
    public Incident updateStatus(Long id, String newStatus) {
        Incident incident = getIncident(id);
        incident.setStatus(newStatus);
        if ("RESOLVED".equals(newStatus) && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        log.info("Incident {} status updated to {}", id, newStatus);
        return incidentRepository.save(incident);
    }

    /**
     * Adds a manual (human-authored) note to an incident's timeline.
     *
     * @param incidentId incident ID
     * @param content    note text (Markdown supported)
     * @param author     username of the author
     */
    @Transactional
    public IncidentNote addManualNote(Long incidentId, String content, String author) {
        // Verify incident exists
        getIncident(incidentId);

        IncidentNote note = IncidentNote.builder()
                .incidentId(incidentId)
                .content(content)
                .noteType("MANUAL")
                .author(author)
                .build();

        return noteRepository.save(note);
    }

    /**
     * Triggers the full AI analysis pipeline for an incident.
     *
     * <p>This method runs the {@link IncidentOrchestrator} which coordinates
     * triage and diagnosis, then persists the results. The operation may take
     * 30–120 seconds depending on the LLM and the number of tool calls.
     *
     * @param incidentId the incident to analyse
     * @return structured analysis result
     * @throws NoSuchElementException if no incident with that ID exists
     */
    @Transactional
    public AnalysisResult analyse(Long incidentId) {
        Incident incident = getIncident(incidentId);
        log.info("Triggering AI analysis for incident {}", incidentId);
        return orchestrator.analyse(incident);
    }
}
