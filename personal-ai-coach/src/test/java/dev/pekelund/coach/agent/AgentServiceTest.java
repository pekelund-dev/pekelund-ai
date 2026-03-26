package dev.pekelund.coach.agent;

import dev.pekelund.coach.domain.AgentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentServiceTest {

    @Test
    void chatReturnsResultWithModelMetadata() {
        // Arrange
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(mock(ChatResponse.class));

        ModelRouter modelRouter = mock(ModelRouter.class);
        when(modelRouter.classify(any())).thenReturn(ModelRouter.TaskComplexity.SIMPLE);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("Test system prompt")
                .build();

        AgentService service = new AgentService(
                chatClient, modelRouter,
                "gemini-2.0-flash", "gemini-2.5-pro",
                1024, 4096);

        // Act
        AgentService.ChatResult result = service.chat("Hej!", "conv-1", AgentType.GENERAL);

        // Assert
        assertThat(result.modelUsed()).isEqualTo("gemini-2.0-flash");
        assertThat(result.complexity()).isEqualTo(ModelRouter.TaskComplexity.SIMPLE);
        assertThat(result.agentType()).isEqualTo(AgentType.GENERAL);
        verify(modelRouter).classify("Hej!");
    }

    @Test
    void chatRoutesToComplexModelWhenClassifiedAsComplex() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(mock(ChatResponse.class));

        ModelRouter modelRouter = mock(ModelRouter.class);
        when(modelRouter.classify(any())).thenReturn(ModelRouter.TaskComplexity.COMPLEX);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("Test")
                .build();

        AgentService service = new AgentService(
                chatClient, modelRouter,
                "gemini-2.0-flash", "gemini-2.5-pro",
                1024, 4096);

        AgentService.ChatResult result = service.chat("Planera min semester", "conv-1", AgentType.VACATION);

        assertThat(result.modelUsed()).isEqualTo("gemini-2.5-pro");
        assertThat(result.complexity()).isEqualTo(ModelRouter.TaskComplexity.COMPLEX);
        assertThat(result.agentType()).isEqualTo(AgentType.VACATION);
    }

    @Test
    void chatResultModelLabelAndIconForSimple() {
        var result = new AgentService.ChatResult("content", "model", ModelRouter.TaskComplexity.SIMPLE, AgentType.GENERAL);
        assertThat(result.modelLabel()).isEqualTo("Gemini Flash");
        assertThat(result.modelIcon()).isEqualTo("⚡");
    }

    @Test
    void chatResultModelLabelAndIconForComplex() {
        var result = new AgentService.ChatResult("content", "model", ModelRouter.TaskComplexity.COMPLEX, AgentType.COACHING);
        assertThat(result.modelLabel()).isEqualTo("Gemini Pro");
        assertThat(result.modelIcon()).isEqualTo("🧠");
    }

    @ParameterizedTest
    @EnumSource(AgentType.class)
    void chatWorksForAllAgentTypes(AgentType agentType) {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(mock(ChatResponse.class));

        ModelRouter modelRouter = mock(ModelRouter.class);
        when(modelRouter.classify(any())).thenReturn(ModelRouter.TaskComplexity.SIMPLE);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("Test")
                .build();

        AgentService service = new AgentService(
                chatClient, modelRouter,
                "gemini-2.0-flash", "gemini-2.5-pro",
                1024, 4096);

        AgentService.ChatResult result = service.chat("test", "conv-1", agentType);
        assertThat(result.agentType()).isEqualTo(agentType);
    }
}
