package dev.pekelund.ai.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.ai.mcp.domain.SimulatedMetric;
import dev.pekelund.ai.mcp.repository.SimulatedMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool for querying service performance metrics.
 *
 * <p>Metric data is the primary signal for detecting the onset and scope of
 * an incident. Agents query CPU, memory, error rate and latency to determine
 * whether a service was under stress before or during the reported incident.
 *
 * <h2>MCP Tool Contract</h2>
 * <ul>
 *   <li>Tool name:  {@code get_service_metrics}</li>
 *   <li>Returns:    JSON array of metric samples (most-recent first)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsQueryTool {

    private final SimulatedMetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves recent performance metrics for a service.
     *
     * <p>Common metrics available in the seed data:
     * <ul>
     *   <li>{@code cpu_percent}         — CPU utilisation (0–100)</li>
     *   <li>{@code memory_percent}      — JVM heap usage (0–100)</li>
     *   <li>{@code error_rate_per_min}  — number of 5xx/exception responses per minute</li>
     *   <li>{@code p95_latency_ms}      — 95th-percentile request latency in ms</li>
     * </ul>
     *
     * @param serviceName  the name of the service (e.g. {@code payment-service})
     * @param metricName   specific metric name, or empty/null to fetch all metrics
     * @param limit        maximum number of data-points to return (1–50, default 10)
     * @return JSON string with an array of metric samples
     */
    @Tool(name = "get_service_metrics",
          description = """
              Retrieve recent performance metrics for a service.
              Available metrics: cpu_percent, memory_percent, error_rate_per_min, p95_latency_ms.
              Returns samples ordered by most-recent first.
              Use this to identify resource exhaustion, traffic spikes or latency regressions
              that correlate with an incident.
              """)
    public String getServiceMetrics(
            @ToolParam(description = "Name of the service, e.g. 'payment-service'")
            String serviceName,
            @ToolParam(description = "Specific metric name (cpu_percent, memory_percent, error_rate_per_min, p95_latency_ms). Leave empty to get all metrics.")
            String metricName,
            @ToolParam(description = "Maximum number of data-points to return (1–50).")
            int limit) {

        log.debug("MCP tool get_service_metrics called: service={}, metric={}, limit={}",
                serviceName, metricName, limit);

        int safeLimit      = Math.min(Math.max(1, limit), 50);
        String metricFilter = (metricName != null && !metricName.isBlank()) ? metricName : null;

        List<SimulatedMetric> metrics = metricRepository.getLatestMetrics(
                serviceName, metricFilter, safeLimit);

        return toJson(metrics.stream().map(m -> Map.of(
                "service",    m.getServiceName(),
                "metric",     m.getMetricName(),
                "value",      m.getValue(),
                "unit",       m.getUnit() != null ? m.getUnit() : "",
                "timestamp",  m.getTimestamp().toString()
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
