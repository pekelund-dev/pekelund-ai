package dev.pekelund.ai.agent.dto;

/**
 * The structured result returned by the {@link dev.pekelund.ai.agent.IncidentOrchestrator}
 * after running a full AI analysis on an incident.
 *
 * @param incidentId       database ID of the analysed incident
 * @param severity         recommended severity level (CRITICAL / HIGH / MEDIUM / LOW)
 * @param category         incident category (DATABASE / NETWORK / MEMORY / CPU / DEPLOYMENT / OTHER)
 * @param affectedServices comma-separated list of identified affected services
 * @param rootCause        concise root-cause statement from the DiagnosisAgent
 * @param executiveSummary high-level narrative for management / post-mortem use
 * @param recommendedSteps numbered list of recommended remediation actions
 * @param runbookReference title of the most relevant runbook (null if none found)
 * @param confidence       AI self-assessed confidence level: HIGH / MEDIUM / LOW
 */
public record AnalysisResult(
        Long incidentId,
        String severity,
        String category,
        String affectedServices,
        String rootCause,
        String executiveSummary,
        String recommendedSteps,
        String runbookReference,
        String confidence
) {
}
