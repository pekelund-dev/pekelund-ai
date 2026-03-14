package dev.pekelund.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.mcp.domain.Runbook;
import dev.pekelund.mcp.repository.RunbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP tool for searching the operational runbook knowledge base.
 *
 * <p>Runbooks encode institutional knowledge about how to handle specific failure modes.
 * By surfacing relevant runbooks, the AI agent can provide concrete, step-by-step
 * recommendations grounded in past human expertise rather than generic advice.
 *
 * <h2>MCP Tool Contract</h2>
 * <ul>
 *   <li>Tool name:  {@code search_runbooks}</li>
 *   <li>Returns:    JSON array of matching runbooks including their full content</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunbookSearchTool {

    private final RunbookRepository runbookRepository;
    private final ObjectMapper objectMapper;

    /**
     * Searches the runbook library by keyword and/or service name.
     *
     * <p>If both {@code keyword} and {@code serviceName} are provided, results
     * are the union (OR semantics) — matching either criterion. This broadens
     * the search so agents are less likely to miss relevant runbooks.
     *
     * @param keyword     word or phrase to search for in title, description and tags
     * @param serviceName filter to runbooks for a specific service
     * @param limit       maximum number of results to return (1–20, default 5)
     * @return JSON string with an array of matching runbooks
     */
    @Tool(name = "search_runbooks",
          description = """
              Search the operational runbook knowledge base.
              Returns runbooks matching a keyword and/or associated with a specific service.
              Use this to find documented procedures for known failure modes such as
              'database connection exhaustion', 'memory leak', 'deployment rollback', etc.
              The runbook content includes step-by-step remediation instructions.
              """)
    public String searchRunbooks(
            @ToolParam(description = "Keyword or phrase to search for in runbook title, description and tags.")
            String keyword,
            @ToolParam(description = "Filter to runbooks for a specific service name. Leave empty to search all services.")
            String serviceName,
            @ToolParam(description = "Maximum number of runbooks to return (1–20).")
            int limit) {

        log.debug("MCP tool search_runbooks called: keyword={}, service={}, limit={}",
                keyword, serviceName, limit);

        int safeLimit = Math.min(Math.max(1, limit), 20);
        List<Runbook> results = new ArrayList<>();

        boolean hasKeyword  = keyword     != null && !keyword.isBlank();
        boolean hasService  = serviceName != null && !serviceName.isBlank();

        if (hasKeyword) {
            results.addAll(runbookRepository.searchByKeyword(keyword, safeLimit));
        }
        if (hasService) {
            runbookRepository.findByServiceNameIgnoreCase(serviceName).stream()
                    .filter(rb -> !results.contains(rb))
                    .limit(safeLimit - results.size())
                    .forEach(results::add);
        }
        if (!hasKeyword && !hasService) {
            results.addAll(runbookRepository.findAll().stream().limit(safeLimit).toList());
        }

        return toJson(results.stream().map(rb -> Map.of(
                "id",          rb.getId(),
                "title",       rb.getTitle(),
                "service",     rb.getServiceName() != null ? rb.getServiceName() : "general",
                "description", rb.getDescription() != null ? rb.getDescription() : "",
                "tags",        rb.getTags()        != null ? rb.getTags()        : "",
                "content",     rb.getContent()
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
