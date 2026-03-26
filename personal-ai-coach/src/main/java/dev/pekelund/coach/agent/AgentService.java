package dev.pekelund.coach.agent;

import dev.pekelund.coach.domain.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates model-routing and chat execution for the Personal AI Coach.
 *
 * <p>Each incoming message is first classified by {@link ModelRouter}:
 * <ul>
 *   <li><b>SIMPLE</b> → {@code gemini-2.0-flash} — snabb och billig.</li>
 *   <li><b>COMPLEX</b> → {@code gemini-2.5-pro} — kraftfull reasoning-modell.</li>
 * </ul>
 *
 * <p>Model names and token limits are configurable via environment variables.
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
            @Value("${coach.agent.simple-model:gemini-2.0-flash}") String simpleModel,
            @Value("${coach.agent.complex-model:gemini-2.5-pro}") String complexModel,
            @Value("${coach.agent.simple-max-tokens:1024}") int simpleMaxTokens,
            @Value("${coach.agent.complex-max-tokens:4096}") int complexMaxTokens) {
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
     * @param agentType      the agent context for the conversation
     * @return {@link ChatResult} containing the response text and model metadata
     */
    public ChatResult chat(String message, String conversationId, AgentType agentType) {
        ModelRouter.TaskComplexity complexity = modelRouter.classify(message);

        String modelName = complexity == ModelRouter.TaskComplexity.COMPLEX ? complexModel : simpleModel;
        GoogleGenAiChatOptions options = buildOptions(complexity);

        log.debug("Routing to {} ({}) for agent {}: {}", modelName, complexity, agentType, message);

        String agentContext = buildAgentContext(agentType);

        String response = chatClient.prompt()
                .system(agentContext)
                .user(message)
                .options(options)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        return new ChatResult(response, modelName, complexity, agentType);
    }

    /**
     * Convenience method that uses the GENERAL agent type.
     */
    public ChatResult chat(String message, String conversationId) {
        return chat(message, conversationId, AgentType.GENERAL);
    }

    private String buildAgentContext(AgentType agentType) {
        return switch (agentType) {
            case CALENDAR -> """
                Du är en kalenderassistent. Hjälp användaren att hantera sina kalendrar,
                visa kommande händelser, skapa nya möten och påminnelser.
                Svara alltid på svenska.""";
            case EMAIL -> """
                Du är en e-postassistent. Hjälp användaren att hantera sin e-post,
                visa viktiga meddelanden, filtrera och markera e-post som kräver åtgärd.
                Svara alltid på svenska.""";
            case FINANCE -> """
                Du är en ekonomiassistent. Hjälp användaren med översikt av aktier,
                sparande, utgifter och ekonomisk planering.
                Svara alltid på svenska.""";
            case MENU -> """
                Du är en menyplaneringsassistent. Hjälp användaren att planera veckomenyerna,
                generera inköpslistor och hitta recept som passar deras preferenser.
                Svara alltid på svenska.""";
            case VACATION -> """
                Du är en semesterplaneringsassistent. Hjälp användaren att planera semester,
                hitta flyg och tåg, jämföra hotellpriser och skapa resplaner.
                Svara alltid på svenska.""";
            case TODO -> """
                Du är en uppgiftshanteringsassistent. Hjälp användaren att hantera sin
                att-göra-lista, skapa, uppdatera och prioritera uppgifter.
                Svara alltid på svenska.""";
            case COACHING -> """
                Du är en personlig coach. Hjälp användaren med målsättning, motivation,
                personlig utveckling och vägledning. Var uppmuntrande men realistisk.
                Svara alltid på svenska.""";
            case GENERAL -> """
                Du är en personlig AI-coach och assistent. Hjälp användaren med alla
                typer av frågor och uppgifter. Var koncis och hjälpsam.
                Svara alltid på svenska.""";
        };
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
     */
    public record ChatResult(
            String content,
            String modelUsed,
            ModelRouter.TaskComplexity complexity,
            AgentType agentType) {

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
