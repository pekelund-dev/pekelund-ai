package dev.pekelund.coach;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.ai.autoconfigure.google.genai.GoogleGenAiAutoConfiguration," +
        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
    "spring.main.allow-bean-definition-overriding=true"
})
class PersonalAiCoachApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ChatModel chatModel() {
            ChatModel mock = mock(ChatModel.class);
            when(mock.call(any(Prompt.class))).thenReturn(mock(ChatResponse.class));
            return mock;
        }

        @Bean
        public EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }

        @Bean
        public VectorStore vectorStore() {
            return mock(VectorStore.class);
        }
    }

    @Test
    void contextLoads() {
        // Verifies the Spring context starts successfully with all beans wired correctly
    }
}
