package dev.pekelund.agent;

import dev.pekelund.agent.domain.Incident;
import dev.pekelund.agent.domain.IncidentNote;
import dev.pekelund.agent.dto.AnalysisResult;
import dev.pekelund.agent.repository.IncidentNoteRepository;
import dev.pekelund.agent.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orchestrator agent that coordinates the full incident analysis pipeline.
 *
 * <h2>Orchestration Flow</h2>
 * <pre>
 * User Request
 *     │
 *     ▼
 * IncidentOrchestrator.analyse(incident)
 *     │
 *     ├─ Step 1: TriageAgent.triage()   → severity, category, affected services
 *     │              └─ MCP tools: get_service_status, search_logs
 *     │
 *     ├─ Step 2: Update incident with triage result (persists to DB)
 *     │
 *     ├─ Step 3: DiagnosisAgent.diagnose() → root cause, evidence, runbook
 *     │              └─ MCP tools: search_logs, get_service_metrics,
 *     │                            search_runbooks, get_incident_history,
 *     │                            get_service_status, add_incident_note
 *     │
 *     ├─ Step 4: Update incident with diagnosis result
 *     │
 *     ├─ Step 5: Synthesise executive summary (ChatClient, no tools)
 *     │
 *     └─ Step 6: Persist final notes and update incident record
 *                     └─ Returns AnalysisResult
 * </pre>
 *
 * <h2>Why an Orchestrator?</h2>
 * <p>Separating orchestration from individual agents follows the <em>single
 * responsibility principle</em>: each agent does one thing well. The orchestrator
 * owns the pipeline, error handling, persistence and the final synthesis step.
 * This also makes it easy to replace or add agents (e.g. a PostMortemAgent) without
 * changing the existing agents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentOrchestrator {

    private final TriageAgent            triageAgent;
    private final DiagnosisAgent         diagnosisAgent;
    private final ChatClient             chatClient;
    private final IncidentRepository     incidentRepository;
    private final IncidentNoteRepository noteRepository;

    /**
     * Runs the full AI analysis pipeline for an incident and persists the results.
     *
     * @param incident the incident to analyse (must be persisted with a valid ID)
     * @return structured {@link AnalysisResult} containing all AI findings
     * @throws IllegalArgumentException if the incident has no ID
     */
    @Transactional
    public AnalysisResult analyse(Incident incident) {
        if (incident.getId() == null) {
            throw new IllegalArgumentException("Incident must be persisted before analysis");
        }

        log.info("Orchestrator starting full analysis pipeline for incident {}", incident.getId());

        // ── Step 1: Triage ────────────────────────────────────────────────────
        log.info("Phase 1/3: Triage");
        String triageResult = triageAgent.triage(incident);

        // Parse triage output and update the incident record
        String severity         = parseField(triageResult, "SEVERITY");
        String category         = parseField(triageResult, "CATEGORY");
        String affectedServices = parseField(triageResult, "AFFECTED_SERVICES");
        String triageReasoning  = parseField(triageResult, "REASONING");

        incident.setSeverity(severity);
        incident.setCategory(category);
        incident.setAffectedServices(affectedServices);
        incident.setStatus("IN_PROGRESS");
        incidentRepository.save(incident);

        persistNote(incident.getId(), "## Triage Assessment\n\n" + triageResult, "TriageAgent");

        // ── Step 2: Diagnosis ─────────────────────────────────────────────────
        log.info("Phase 2/3: Diagnosis");
        String diagnosisResult = diagnosisAgent.diagnose(incident);

        String rootCause        = parseField(diagnosisResult, "ROOT_CAUSE");
        String runbookReference = parseField(diagnosisResult, "RECOMMENDED_RUNBOOK");

        incident.setRootCause(rootCause);
        incidentRepository.save(incident);

        persistNote(incident.getId(), "## Root Cause Analysis\n\n" + diagnosisResult, "DiagnosisAgent");

        // ── Step 3: Executive Summary ─────────────────────────────────────────
        log.info("Phase 3/3: Executive Summary");
        String executiveSummary = synthesiseExecutiveSummary(incident, triageResult, diagnosisResult);

        incident.setAiSummary(executiveSummary);
        incidentRepository.save(incident);

        persistNote(incident.getId(), "## Executive Summary\n\n" + executiveSummary, "IncidentOrchestrator");

        log.info("Orchestrator completed full analysis for incident {}", incident.getId());

        return new AnalysisResult(
                incident.getId(),
                severity,
                category,
                affectedServices,
                rootCause,
                executiveSummary,
                parseField(diagnosisResult, "CONTRIBUTING_FACTORS"),
                "NONE".equals(runbookReference) ? null : runbookReference,
                parseField(triageResult, "CONFIDENCE")
        );
    }

    /**
     * Generates a concise executive summary from the triage and diagnosis outputs.
     *
     * <p>This step uses a {@code ChatClient} call <em>without</em> tools — the LLM
     * synthesises from the information already gathered rather than making new tool calls.
     * This is intentional: synthesis should be deterministic and based solely on the
     * evidence already in context, not on additional queries that could be non-deterministic.
     */
    private String synthesiseExecutiveSummary(Incident incident,
                                               String triageResult,
                                               String diagnosisResult) {
        String prompt = """
                You are writing an executive summary for an IT incident post-mortem.
                Based on the triage and diagnosis below, write a 3-4 sentence executive summary
                suitable for a non-technical audience. Include: what happened, business impact,
                root cause in plain English, and recommended next step.

                INCIDENT: %s
                DESCRIPTION: %s

                TRIAGE:
                %s

                DIAGNOSIS:
                %s
                """.formatted(
                incident.getTitle(),
                incident.getDescription(),
                triageResult,
                diagnosisResult
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * Extracts a field value from a structured agent response.
     *
     * <p>Fields are expected in the format: {@code FIELD_NAME: value} on a single line.
     * Multi-line values (e.g. CONTRIBUTING_FACTORS) are captured until the next
     * all-uppercase field or end of string.
     *
     * @param text      the raw agent response
     * @param fieldName the field name to extract (e.g. "SEVERITY")
     * @return the extracted value, or "UNKNOWN" if the field is not present
     */
    static String parseField(String text, String fieldName) {
        if (text == null) {
            return "UNKNOWN";
        }
        String[] lines = text.split("\n");
        StringBuilder value = new StringBuilder();
        boolean capturing = false;

        for (String line : lines) {
            if (line.startsWith(fieldName + ":")) {
                value.append(line.substring(fieldName.length() + 1).trim());
                capturing = true;
            } else if (capturing) {
                // Stop at the next FIELD_NAME: line
                if (line.matches("^[A-Z_]+:.*")) {
                    break;
                }
                if (!line.isBlank()) {
                    value.append("\n").append(line);
                }
            }
        }

        String result = value.toString().trim();
        return result.isBlank() ? "UNKNOWN" : result;
    }

    private void persistNote(Long incidentId, String content, String author) {
        noteRepository.save(IncidentNote.builder()
                .incidentId(incidentId)
                .content(content)
                .noteType("AUTO")
                .author(author)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
