package dev.pekelund.coach.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Klassificerar inkommande meddelanden som SIMPLE eller COMPLEX med en snabb,
 * billig modell ({@code gemini-2.0-flash-lite} som standard), så att huvudagenten
 * kan dirigera till lämplig modelltier utan att slösa tokens.
 *
 * <p>Komplexitetskategorier:
 * <ul>
 *   <li><b>SIMPLE</b> — Enkla uppslag: väder, tid, läsa ett e-postmeddelande,
 *       enkel kalenderkontroll, visa att-göra-lista.</li>
 *   <li><b>COMPLEX</b> — Flerstegsuppgifter, skrivande, analys, resonemang,
 *       eller operationer som berör flera tjänster.</li>
 * </ul>
 */
@Service
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private static final String ROUTER_SYSTEM_PROMPT = """
            Classify the following user request as either SIMPLE or COMPLEX.

            SIMPLE: single-fact lookups — weather, current time, reading one email,
            basic calendar check, viewing a todo list, simple recipe lookup.

            COMPLEX: multi-step tasks, email drafting, writing, analysis, comparing
            information from multiple sources, scheduling with constraints, menu planning,
            financial analysis, vacation planning, coaching conversations, or any request
            that requires reasoning across several steps.

            Respond with exactly one word: SIMPLE or COMPLEX.
            """;

    private final ChatClient routerClient;

    public ModelRouter(
            ChatModel chatModel,
            @Value("${coach.agent.router-model:gemini-2.0-flash-lite}") String routerModel) {
        this.routerClient = ChatClient.builder(chatModel)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(routerModel)
                        .maxOutputTokens(5)
                        .temperature(0.0)
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
        /** Enkla uppslag — hanteras av gemini-2.0-flash. */
        SIMPLE,
        /** Komplext resonemang — hanteras av gemini-2.5-pro. */
        COMPLEX
    }
}
