package dev.pekelund.personal.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Classifies an incoming user message as SIMPLE or COMPLEX using a cheap, fast model
 * ({@code gemini-2.0-flash-lite} by default), so the main agent can route to the
 * appropriate model tier without wasting tokens.
 *
 * <p>Complexity categories:
 * <ul>
 *   <li><b>SIMPLE</b> – single-fact lookups: weather, current time, reading one email,
 *       basic calendar check, simple note retrieval.</li>
 *   <li><b>COMPLEX</b> – multi-step tasks, drafting/writing, analysis, reasoning,
 *       or operations that touch multiple services.</li>
 * </ul>
 */
@Service
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private static final String ROUTER_SYSTEM_PROMPT = """
            Classify the following user request as either SIMPLE or COMPLEX.

            SIMPLE: single-fact lookups — weather, current time, reading one email,
            basic calendar check, simple note retrieval, asking a straightforward question.

            COMPLEX: multi-step tasks, email drafting, writing, analysis, comparing
            information from multiple sources, scheduling with constraints, or any request
            that requires reasoning across several steps.

            Respond with exactly one word: SIMPLE or COMPLEX.
            """;

    private final ChatClient routerClient;

    public ModelRouter(
            ChatModel chatModel,
            @Value("${personal.agent.router-model:gemini-2.0-flash-lite}") String routerModel) {
        // The router client has no tools and no memory — it only classifies
        this.routerClient = ChatClient.builder(chatModel)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(routerModel)
                        .maxOutputTokens(5)      // only "SIMPLE" or "COMPLEX" needed
                        .temperature(0.0)         // deterministic classification
                        .build())
                .build();
        log.info("ModelRouter using classifier model: {}", routerModel);
    }

    /**
     * Classifies the given user message. Falls back to {@link TaskComplexity#SIMPLE}
     * on any error so a routing failure never blocks the user.
     */
    public TaskComplexity classify(String userMessage) {
        try {
            String result = routerClient.prompt()
                    .system(ROUTER_SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
            TaskComplexity complexity = "COMPLEX".equals(result != null ? result.trim().toUpperCase() : "")
                    ? TaskComplexity.COMPLEX
                    : TaskComplexity.SIMPLE;
            log.debug("Classified '{}' as {}", userMessage, complexity);
            return complexity;
        } catch (Exception e) {
            log.warn("Task classification failed, defaulting to SIMPLE: {}", e.getMessage());
            return TaskComplexity.SIMPLE;
        }
    }

    public enum TaskComplexity {
        /** Simple lookups handled by gemini-2.0-flash. */
        SIMPLE,
        /** Complex reasoning handled by gemini-2.5-pro. */
        COMPLEX
    }
}
