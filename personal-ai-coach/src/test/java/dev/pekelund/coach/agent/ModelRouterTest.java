package dev.pekelund.coach.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelRouterTest {

    @Test
    void classifyReturnsSimpleForSimpleQuery() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        ModelRouter router = new ModelRouter(chatModel, "gemini-2.0-flash-lite");

        // The mock returns null content, which should default to SIMPLE
        ModelRouter.TaskComplexity result = router.classify("Vad är klockan?");
        assertThat(result).isEqualTo(ModelRouter.TaskComplexity.SIMPLE);
    }

    @Test
    void classifyDefaultsToSimpleOnError() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        ModelRouter router = new ModelRouter(chatModel, "gemini-2.0-flash-lite");

        ModelRouter.TaskComplexity result = router.classify("Any message");
        assertThat(result).isEqualTo(ModelRouter.TaskComplexity.SIMPLE);
    }
}
