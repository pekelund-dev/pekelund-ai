package dev.pekelund.ai.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.ai.mcp.domain.SimulatedLog;
import dev.pekelund.ai.mcp.repository.SimulatedLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool for querying simulated application logs.
 *
 * <p>AI agents call this tool to find error bursts, stack traces, and unusual
 * patterns that correlate with an incident's reported symptoms. The results help
 * the DiagnosisAgent form hypotheses about the root cause.
 *
 * <h2>MCP Tool Contract</h2>
 * <ul>
 *   <li>Tool name:  {@code search_logs}</li>
 *   <li>Returns:    JSON array of matching log entries (up to {@code limit})</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogQueryTool {

    private final SimulatedLogRepository logRepository;
    private final ObjectMapper objectMapper;

    /**
     * Searches application logs for a specific service with optional filters.
     *
     * <p>Example agent prompt: "Search the logs of the payment-service for any
     * ERROR entries mentioning 'timeout' in the last request cycle."
     *
     * @param serviceName the name of the service whose logs to search (e.g. {@code payment-service})
     * @param level       optional log level filter: {@code ERROR}, {@code WARN}, {@code INFO},
     *                    {@code DEBUG}. Pass {@code null} or empty to match all levels.
     * @param pattern     optional substring to match against the log message body.
     *                    Case-insensitive. Pass {@code null} or empty to match all messages.
     * @param limit       maximum number of results to return (1–100, default 20)
     * @return JSON string containing a list of matching log entries with their service name,
     *         level, message, metadata and timestamp.
     */
    @Tool(name = "search_logs",
          description = """
              Search application logs for a given service.
              Returns a JSON list of log entries ordered by most-recent first.
              Use this tool to find error bursts, exception stack traces, slow-query
              warnings, and other anomalies that coincide with an incident.
              """)
    public String searchLogs(
            @ToolParam(description = "Name of the service to search logs for, e.g. 'payment-service'")
            String serviceName,
            @ToolParam(description = "Log level filter: ERROR, WARN, INFO or DEBUG. Leave empty for all levels.")
            String level,
            @ToolParam(description = "Substring to search for in log messages. Case-insensitive. Leave empty for all messages.")
            String pattern,
            @ToolParam(description = "Maximum number of log entries to return (1–100).")
            int limit) {

        log.debug("MCP tool search_logs called: service={}, level={}, pattern={}, limit={}",
                serviceName, level, pattern, limit);

        int safeLimit = Math.min(Math.max(1, limit), 100);
        String levelFilter  = (level   != null && !level.isBlank())   ? level   : null;
        String patternFilter = (pattern != null && !pattern.isBlank()) ? pattern : null;

        List<SimulatedLog> logs = logRepository.searchLogs(
                serviceName, levelFilter, patternFilter, safeLimit);

        return toJson(logs.stream().map(l -> Map.of(
                "id",          l.getId(),
                "service",     l.getServiceName(),
                "level",       l.getLevel(),
                "message",     l.getMessage(),
                "metadata",    l.getMetadata() != null ? l.getMetadata() : "{}",
                "timestamp",   l.getTimestamp().toString()
        )).toList());
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
