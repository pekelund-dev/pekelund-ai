package dev.pekelund.personal.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates model-routing and chat execution.
 *
 * <p>Each incoming message is first classified by {@link ModelRouter}:
 * <ul>
 *   <li><b>SIMPLE</b> → {@code gemini-2.0-flash} (default) — fast, cheap, excellent tool-calling.</li>
 *   <li><b>COMPLEX</b> → {@code gemini-2.5-pro} (default) — powerful reasoning model for
 *       multi-step tasks, drafting, and analysis.</li>
 * </ul>
 *
 * <p>Model names and token limits are configurable via environment variables:
 * {@code GEMINI_SIMPLE_MODEL}, {@code GEMINI_COMPLEX_MODEL},
 * {@code GEMINI_SIMPLE_MAX_TOKENS}, {@code GEMINI_COMPLEX_MAX_TOKENS}.
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ChatClient chatClient;
    private final ModelRouter modelRouter;
    private final String simpleModel;
    private final String complexModel;
    private final int simpleMaxTokens;
    private final int complexMaxTokens;

    public AgentService(
            ChatClient chatClient,
            ModelRouter modelRouter,
            @Value("${personal.agent.simple-model:gemini-2.0-flash}") String simpleModel,
            @Value("${personal.agent.complex-model:gemini-2.5-pro}") String complexModel,
            @Value("${personal.agent.simple-max-tokens:1024}") int simpleMaxTokens,
            @Value("${personal.agent.complex-max-tokens:4096}") int complexMaxTokens) {
        this.chatClient = chatClient;
        this.modelRouter = modelRouter;
        this.simpleModel = simpleModel;
        this.complexModel = complexModel;
        this.simpleMaxTokens = simpleMaxTokens;
        this.complexMaxTokens = complexMaxTokens;
        log.info("AgentService: simple-model={} (max {}t), complex-model={} (max {}t)",
                simpleModel, simpleMaxTokens, complexModel, complexMaxTokens);
    }

    /**
     * Classifies the user message, selects the appropriate Gemini model, and returns
     * the AI response together with the model that was used.
     *
     * @param message        the user's message
     * @param conversationId the conversation ID used for chat memory
     * @return {@link ChatResult} containing the response text and model metadata
     */
    public ChatResult chat(String message, String conversationId) {
        ModelRouter.TaskComplexity complexity = modelRouter.classify(message);

        String modelName = complexity == ModelRouter.TaskComplexity.COMPLEX ? complexModel : simpleModel;
        GoogleGenAiChatOptions options = buildOptions(complexity);

        log.debug("Routing to {} ({}) for: {}", modelName, complexity, message);

        String response = chatClient.prompt()
                .user(message)
                .options(options)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        return new ChatResult(response, modelName, complexity);
    }

    private GoogleGenAiChatOptions buildOptions(ModelRouter.TaskComplexity complexity) {
        if (complexity == ModelRouter.TaskComplexity.COMPLEX) {
            return GoogleGenAiChatOptions.builder()
                    .model(complexModel)
                    .maxOutputTokens(complexMaxTokens)
                    .build();
        }
        return GoogleGenAiChatOptions.builder()
                .model(simpleModel)
                .maxOutputTokens(simpleMaxTokens)
                .build();
    }

    /**
     * The result of an agent chat call, including the response content and the model
     * that was selected for the request.
     *
     * @param content    the AI-generated response text
     * @param modelUsed  the Gemini model ID that produced the response
     * @param complexity the task complexity classification used to select the model
     */
    public record ChatResult(
            String content,
            String modelUsed,
            ModelRouter.TaskComplexity complexity) {

        /** Returns a short human-readable label for display in the UI. */
        public String modelLabel() {
            return complexity == ModelRouter.TaskComplexity.COMPLEX ? "Gemini Pro" : "Gemini Flash";
        }

        /** Returns an icon character for the model tier. */
        public String modelIcon() {
            return complexity == ModelRouter.TaskComplexity.COMPLEX ? "🧠" : "⚡";
        }
    }
}
