package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pekelund.ai.agent.domain.Incident;
import dev.pekelund.ai.agent.dto.AnalysisResult;
import dev.pekelund.ai.agent.dto.CreateIncidentRequest;
import dev.pekelund.ai.agent.service.IncidentService;
import dev.pekelund.ai.agent.TriageAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link A2AController}.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>The agent card is well-formed and contains the expected skills</li>
 *   <li>JSON-RPC request validation (missing required fields, unknown methods)</li>
 *   <li>{@code tasks/send} succeeds for both skills (incident_analysis, incident_triage)</li>
 *   <li>{@code tasks/get} retrieves a cached task and returns 404 for unknown task IDs</li>
 *   <li>{@code tasks/cancel} returns the correct error</li>
 * </ul>
 *
 * <p>All AI calls are mocked so these tests run without an API key or MCP server.
 */
@ExtendWith(MockitoExtension.class)
class A2AControllerTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private TriageAgent triageAgent;

    private ObjectMapper objectMapper;
    private A2ATaskService taskService;
    private A2AController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        taskService  = new A2ATaskService(incidentService, triageAgent);
        controller   = new A2AController(taskService, objectMapper);
    }

    // ── Agent Card ─────────────────────────────────────────────────────────────

    @Test
    void agentCard_returnsWellFormedCard() {
        A2AAgentCard card = controller.agentCard();

        assertThat(card.name()).isNotBlank();
        assertThat(card.url()).isEqualTo("/a2a");
        assertThat(card.version()).isNotBlank();
        assertThat(card.skills()).hasSize(2);
        assertThat(card.skills()).extracting(A2ASkill::id)
                .containsExactlyInAnyOrder("incident_analysis", "incident_triage");
        assertThat(card.defaultInputModes()).contains("text/plain");
        assertThat(card.defaultOutputModes()).contains("text/plain");
    }

    @Test
    void agentCard_capabilitiesAreCorrect() {
        A2AAgentCard card = controller.agentCard();

        // This implementation is synchronous — streaming and push notifications are false
        assertThat(card.capabilities().streaming()).isFalse();
        assertThat(card.capabilities().pushNotifications()).isFalse();
    }

    // ── JSON-RPC Validation ────────────────────────────────────────────────────

    @Test
    void handleTask_rejectsInvalidJsonrpcVersion() {
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("1.0", "req-1", "tasks/send", null);

        A2AJsonRpcResponse response = controller.handleTask(request);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(A2AJsonRpcResponse.ErrorCode.INVALID_REQUEST.code);
        assertThat(response.result()).isNull();
    }

    @Test
    void handleTask_rejectsUnknownMethod() {
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-1", "tasks/unknown", null);

        A2AJsonRpcResponse response = controller.handleTask(request);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(A2AJsonRpcResponse.ErrorCode.METHOD_NOT_FOUND.code);
    }

    @Test
    void handleTask_cancelReturnsCorrectError() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("id", "task-123");
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-1", "tasks/cancel", params);

        A2AJsonRpcResponse response = controller.handleTask(request);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code())
                .isEqualTo(A2AJsonRpcResponse.ErrorCode.TASK_CANNOT_BE_CANCELLED.code);
    }

    // ── tasks/send ─────────────────────────────────────────────────────────────

    @Test
    void tasksSend_fullAnalysis_returnsCompletedTask() {
        // Arrange
        Incident savedIncident = Incident.builder().id(42L).title("Test").description("desc").build();
        AnalysisResult analysisResult = new AnalysisResult(
                42L, "HIGH", "DATABASE", "payment-service",
                "Connection pool exhausted", "DB connections at 100%",
                "Restart connection pool", "MEDIUM", "Runbook: DB-001");
        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(savedIncident);
        when(incidentService.analyse(anyLong())).thenReturn(analysisResult);

        ObjectNode params = buildTaskSendParams("task-001", "incident_analysis",
                "title: Payment 503\ndescription: Error rate 80%");
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-1", "tasks/send", params);

        // Act
        A2AJsonRpcResponse response = controller.handleTask(request);

        // Assert
        assertThat(response.error()).isNull();
        assertThat(response.result()).isInstanceOf(A2ATask.class);

        A2ATask task = (A2ATask) response.result();
        assertThat(task.id()).isEqualTo("task-001");
        assertThat(task.status().state()).isEqualTo("completed");
        assertThat(task.status().message().role()).isEqualTo("agent");
        assertThat(task.status().message().parts()).isNotEmpty();
        assertThat(task.metadata()).containsKey("incidentId");
    }

    @Test
    void tasksSend_triageOnly_returnsCompletedTask() {
        // Arrange
        when(triageAgent.triage(any(Incident.class)))
                .thenReturn("SEVERITY: HIGH\nCATEGORY: DATABASE\nAFFECTED_SERVICES: payment-service");

        ObjectNode params = buildTaskSendParams("task-002", "incident_triage",
                "title: Payment 503\ndescription: Error rate 80%");
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-2", "tasks/send", params);

        // Act
        A2AJsonRpcResponse response = controller.handleTask(request);

        // Assert
        assertThat(response.error()).isNull();
        A2ATask task = (A2ATask) response.result();
        assertThat(task.id()).isEqualTo("task-002");
        assertThat(task.status().state()).isEqualTo("completed");
        assertThat(task.status().message().parts().get(0).text())
                .contains("SEVERITY: HIGH");
    }

    @Test
    void tasksSend_missingMessage_returnsInvalidParams() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("id", "task-003");
        // No "message" field
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-3", "tasks/send", params);

        A2AJsonRpcResponse response = controller.handleTask(request);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(A2AJsonRpcResponse.ErrorCode.INVALID_PARAMS.code);
    }

    // ── tasks/get ──────────────────────────────────────────────────────────────

    @Test
    void tasksGet_returnsCompletedTask() {
        // First send a task (triage) so it ends up in the cache
        when(triageAgent.triage(any(Incident.class))).thenReturn("SEVERITY: LOW");
        ObjectNode sendParams = buildTaskSendParams("task-get-1", "incident_triage",
                "title: Minor alert\ndescription: Low traffic drop");
        controller.handleTask(new A2AJsonRpcRequest("2.0", "x", "tasks/send", sendParams));

        // Then retrieve it
        ObjectNode getParams = objectMapper.createObjectNode();
        getParams.put("id", "task-get-1");
        A2AJsonRpcRequest getRequest = new A2AJsonRpcRequest("2.0", "req-get", "tasks/get", getParams);

        A2AJsonRpcResponse getResponse = controller.handleTask(getRequest);

        assertThat(getResponse.error()).isNull();
        A2ATask retrieved = (A2ATask) getResponse.result();
        assertThat(retrieved.id()).isEqualTo("task-get-1");
        assertThat(retrieved.status().state()).isEqualTo("completed");
    }

    @Test
    void tasksGet_unknownTaskId_returnsTaskNotFound() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("id", "non-existent-task");
        A2AJsonRpcRequest request = new A2AJsonRpcRequest("2.0", "req-nf", "tasks/get", params);

        A2AJsonRpcResponse response = controller.handleTask(request);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(A2AJsonRpcResponse.ErrorCode.TASK_NOT_FOUND.code);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ObjectNode buildTaskSendParams(String taskId, String skillId, String text) {
        ObjectNode params  = objectMapper.createObjectNode();
        params.put("id",      taskId);
        params.put("skillId", skillId);

        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "text/plain");
        part.put("text", text);
        message.set("parts", objectMapper.createArrayNode().add(part));
        params.set("message", message);
        return params;
    }
}
