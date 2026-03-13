package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A2A (Agent-to-Agent) Protocol controller.
 *
 * <p>Implements the two mandatory A2A HTTP endpoints:
 * <ol>
 *   <li>{@code GET /.well-known/agent.json} — Agent Card discovery endpoint</li>
 *   <li>{@code POST /a2a}                   — JSON-RPC 2.0 task operations</li>
 * </ol>
 *
 * <h2>A2A Protocol Flow</h2>
 * <pre>
 * Client                                       ITII Agent
 *   │                                               │
 *   │  GET /.well-known/agent.json                  │
 *   │ ─────────────────────────────────────────────▶│
 *   │  ◀ AgentCard (skills, url, capabilities)      │
 *   │                                               │
 *   │  POST /a2a  (tasks/send, id=T1)               │
 *   │  { role:"user", parts:[{type:"text",text:…}]} │
 *   │ ─────────────────────────────────────────────▶│
 *   │                             [run AI analysis] │
 *   │  ◀ { id:T1, status:completed, message:{…} }   │
 *   │                                               │
 *   │  POST /a2a  (tasks/get, id=T1)                │
 *   │ ─────────────────────────────────────────────▶│
 *   │  ◀ same completed task (cached)               │
 * </pre>
 *
 * <h2>Supported JSON-RPC Methods</h2>
 * <ul>
 *   <li>{@code tasks/send}   — submit a task (see {@link #handleTaskSend})</li>
 *   <li>{@code tasks/get}    — retrieve a completed task (see {@link #handleTaskGet})</li>
 *   <li>{@code tasks/cancel} — not supported; returns error</li>
 * </ul>
 *
 * <h2>Skill Routing</h2>
 * <p>Pass an optional {@code skillId} in the request params to select a specific skill:
 * <ul>
 *   <li>{@code incident_triage}   — fast triage only, no persistence</li>
 *   <li>{@code incident_analysis} — full pipeline (default)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class A2AController {

    private static final String AGENT_VERSION     = "1.0.0";
    private static final String TASK_ENDPOINT_URL = "/a2a";

    private final A2ATaskService taskService;
    private final ObjectMapper   objectMapper;

    /**
     * Returns the Agent Card — the A2A discovery document.
     *
     * <p>This endpoint MUST be at {@code /.well-known/agent.json} per the A2A spec.
     * Any A2A-compatible orchestrator (e.g. another agent or an A2A client library)
     * will hit this URL first to understand what the agent can do.
     */
    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public A2AAgentCard agentCard() {
        return new A2AAgentCard(
                "ITII Incident Analysis Agent",
                "AI-powered IT incident triage and root-cause analysis. " +
                "Provide an incident title and description and the agent will diagnose " +
                "the root cause, assess severity and recommend remediation steps.",
                TASK_ENDPOINT_URL,
                AGENT_VERSION,
                new A2AAgentCard.A2ACapabilities(false, false),
                List.of(
                        new A2ASkill(
                                "incident_analysis",
                                "Full Incident Analysis",
                                "Creates a persistent incident record and runs the full 3-phase AI pipeline: " +
                                "triage (severity/category/affected services), root-cause diagnosis using MCP tools " +
                                "(logs, metrics, runbooks, history), and executive summary generation. " +
                                "Returns a structured report with root cause, evidence and remediation steps.",
                                List.of("text/plain"),
                                List.of("text/plain"),
                                List.of(
                                        "title: Payment service returning 503\ndescription: Error rate at 80%, started 5 minutes ago",
                                        "title: Fraud service OOM crash\ndescription: Pod restarting every 90 minutes since v2.4.1 deployment"
                                ),
                                List.of("incident", "triage", "diagnosis", "root-cause", "sre")
                        ),
                        new A2ASkill(
                                "incident_triage",
                                "Fast Incident Triage",
                                "Rapid triage only — no database persistence. " +
                                "Returns severity (CRITICAL/HIGH/MEDIUM/LOW), incident category and affected services. " +
                                "Use this when you need a quick first assessment without creating a permanent incident record.",
                                List.of("text/plain"),
                                List.of("text/plain"),
                                List.of(
                                        "title: API gateway 503\ndescription: All endpoints returning 503 since 15 minutes"
                                ),
                                List.of("incident", "triage", "sre")
                        )
                ),
                List.of("text/plain"),
                List.of("text/plain")
        );
    }

    /**
     * JSON-RPC 2.0 task endpoint — the main A2A interaction point.
     *
     * <p>Dispatches to the appropriate handler based on the {@code method} field.
     * Returns HTTP 200 for both success and application-level JSON-RPC errors (per spec).
     */
    @PostMapping(value = "/a2a",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public A2AJsonRpcResponse handleTask(@RequestBody A2AJsonRpcRequest request) {
        log.debug("A2A request: method={}, id={}", request.method(), request.id());

        if (request.jsonrpc() == null || !"2.0".equals(request.jsonrpc())) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_REQUEST,
                    "jsonrpc must be '2.0'");
        }

        if (request.method() == null) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_REQUEST,
                    "method is required");
        }

        return switch (request.method()) {
            case "tasks/send"   -> handleTaskSend(request);
            case "tasks/get"    -> handleTaskGet(request);
            case "tasks/cancel" -> A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.TASK_CANNOT_BE_CANCELLED,
                    "Synchronous tasks cannot be cancelled after submission");
            default             -> A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.METHOD_NOT_FOUND,
                    "Unknown method: " + request.method());
        };
    }

    // ── Private Handlers ──────────────────────────────────────────────────────

    /**
     * Handles {@code tasks/send} — creates and immediately executes a task.
     *
     * <p>Expected params:
     * <pre>{@code
     * {
     *   "id":       "uuid",              // required: client-generated task ID
     *   "skillId":  "incident_analysis", // optional: defaults to incident_analysis
     *   "message": {                     // required: the user's input
     *     "role": "user",
     *     "parts": [{"type": "text/plain", "text": "title: ...\ndescription: ..."}]
     *   }
     * }
     * }</pre>
     */
    private A2AJsonRpcResponse handleTaskSend(A2AJsonRpcRequest request) {
        JsonNode params = request.params();
        if (params == null) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS, "params is required");
        }

        // Extract task ID — generate one if not provided
        String taskId = params.has("id") ? params.get("id").asText() : UUID.randomUUID().toString();

        // Extract skillId (optional)
        String skillId = params.has("skillId") ? params.get("skillId").asText() : null;

        // Extract and deserialise the user message
        if (!params.has("message")) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS, "params.message is required");
        }
        A2AMessage userMessage;
        try {
            userMessage = objectMapper.treeToValue(params.get("message"), A2AMessage.class);
        } catch (Exception e) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS,
                    "Invalid message format: " + e.getMessage());
        }

        if (userMessage.parts() == null || userMessage.parts().isEmpty()) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS,
                    "message.parts must contain at least one text part");
        }

        log.info("A2A tasks/send: taskId={}, skillId={}", taskId, skillId);
        A2ATask task = taskService.processTask(taskId, userMessage, skillId);
        return A2AJsonRpcResponse.success(request.id(), task);
    }

    /**
     * Handles {@code tasks/get} — retrieves a previously processed task from the cache.
     *
     * <p>Expected params: {@code {"id": "task-uuid"}}
     */
    private A2AJsonRpcResponse handleTaskGet(A2AJsonRpcRequest request) {
        JsonNode params = request.params();
        if (params == null || !params.has("id")) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS, "params.id is required");
        }

        String taskId = params.get("id").asText();
        Optional<A2ATask> task = taskService.getTask(taskId);

        if (task.isEmpty()) {
            return A2AJsonRpcResponse.error(request.id(),
                    A2AJsonRpcResponse.ErrorCode.TASK_NOT_FOUND,
                    "Task not found: " + taskId);
        }

        return A2AJsonRpcResponse.success(request.id(), task.get());
    }
}
