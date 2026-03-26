package dev.pekelund.coach.controller;

import dev.pekelund.coach.agent.AgentService;
import dev.pekelund.coach.agent.ModelRouter;
import dev.pekelund.coach.domain.AgentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiController}.
 */
class ApiControllerTest {

    private final AgentService agentService = mock(AgentService.class);
    private final ApiController controller = new ApiController(agentService);

    @Test
    void chatReturnsResponseWithDefaults() {
        when(agentService.chat(anyString(), anyString(), any(AgentType.class)))
                .thenReturn(new AgentService.ChatResult(
                        "Hej! Jag kan hjälpa dig.",
                        "gemini-2.0-flash",
                        ModelRouter.TaskComplexity.SIMPLE,
                        AgentType.GENERAL));

        var response = controller.chat(
                new ApiController.ChatRequest("Hej!", null, null));

        assertThat(response.response()).isEqualTo("Hej! Jag kan hjälpa dig.");
        assertThat(response.modelUsed()).isEqualTo("gemini-2.0-flash");
        assertThat(response.agentType()).isEqualTo(AgentType.GENERAL);
        assertThat(response.conversationId()).isNotNull();
    }

    @Test
    void chatUsesProvidedConversationIdAndAgentType() {
        when(agentService.chat(anyString(), eq("my-conv"), eq(AgentType.COACHING)))
                .thenReturn(new AgentService.ChatResult(
                        "Coaching svar",
                        "gemini-2.5-pro",
                        ModelRouter.TaskComplexity.COMPLEX,
                        AgentType.COACHING));

        var response = controller.chat(
                new ApiController.ChatRequest("Hjälp mig med mål", "my-conv", AgentType.COACHING));

        assertThat(response.conversationId()).isEqualTo("my-conv");
        assertThat(response.agentType()).isEqualTo(AgentType.COACHING);
        assertThat(response.modelUsed()).isEqualTo("gemini-2.5-pro");
    }
}
