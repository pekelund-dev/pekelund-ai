package dev.pekelund.ai.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.ai.mcp.domain.ServiceEntity;
import dev.pekelund.ai.mcp.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP tool for querying service registry and status information.
 *
 * <p>Before an agent can investigate logs and metrics it needs to understand the
 * service landscape: which services exist, which are critical, who owns them and
 * what do they depend on. This tool exposes that context.
 *
 * <h2>MCP Tool Contract</h2>
 * <ul>
 *   <li>Tool name:  {@code get_service_status}</li>
 *   <li>Returns:    JSON object (single service) or JSON array (all services)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceStatusTool {

    private final ServiceEntityRepository serviceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves service metadata from the service registry.
     *
     * <p>When {@code serviceName} is provided, returns the metadata for that one service.
     * When left empty, returns a summary list of all registered services — useful for
     * the TriageAgent to understand which services exist when classifying a vague incident.
     *
     * @param serviceName name of the service, or empty to list all services
     * @return JSON with service details: name, team, description, criticality and dependencies
     */
    @Tool(name = "get_service_status",
          description = """
              Retrieve service registry information for one or all services.
              Returns team ownership, description, criticality and upstream dependencies.
              Use this to understand which service is involved in an incident,
              who to contact, and what other services might be impacted downstream.
              Leave serviceName empty to get a summary list of all services.
              """)
    public String getServiceStatus(
            @ToolParam(description = "Name of the service (e.g. 'payment-service'). Leave empty to list all services.")
            String serviceName) {

        log.debug("MCP tool get_service_status called: service={}", serviceName);

        if (serviceName != null && !serviceName.isBlank()) {
            Optional<ServiceEntity> service = serviceRepository.findByName(serviceName);
            if (service.isEmpty()) {
                return "{\"error\": \"Service '" + serviceName + "' not found in registry\"}";
            }
            return toJson(toMap(service.get()));
        }

        // Return summary of all services
        List<ServiceEntity> all = serviceRepository.findAll();
        return toJson(all.stream().map(this::toMap).toList());
    }

    private Map<String, Object> toMap(ServiceEntity s) {
        return Map.of(
                "name",         s.getName(),
                "team",         s.getTeam()         != null ? s.getTeam()         : "Unknown",
                "description",  s.getDescription()  != null ? s.getDescription()  : "",
                "critical",     s.isCritical(),
                "dependencies", s.getDependencies()  != null ? s.getDependencies() : ""
        );
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
