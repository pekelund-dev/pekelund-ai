package dev.pekelund.personal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.google.genai.api-key=test-key",
    "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.google.genai.GoogleGenAiAutoConfiguration"
})
class PersonalAiAgentApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ChatModel chatModel() {
            ChatModel mock = mock(ChatModel.class);
            when(mock.call(any(Prompt.class))).thenReturn(mock(ChatResponse.class));
            return mock;
        }
    }

    @Test
    void contextLoads() {
        // Verifies the Spring context starts successfully with all beans wired correctly
        assertThat(true).isTrue();
    }
}
