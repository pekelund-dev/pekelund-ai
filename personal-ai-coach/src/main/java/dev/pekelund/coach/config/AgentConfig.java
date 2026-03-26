package dev.pekelund.coach.config;

import dev.pekelund.coach.agent.calendar.CalendarAgent;
import dev.pekelund.coach.agent.coaching.CoachingAgent;
import dev.pekelund.coach.agent.email.EmailAgent;
import dev.pekelund.coach.agent.finance.FinanceAgent;
import dev.pekelund.coach.agent.menu.MenuPlanningAgent;
import dev.pekelund.coach.agent.todo.TodoAgent;
import dev.pekelund.coach.agent.vacation.VacationAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient with tools, memory, and the Swedish system prompt.
 *
 * <p>The ChatClient is shared across all agents but each agent provides its own
 * system prompt and model selection at call time.
 */
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            CalendarAgent calendarAgent,
            EmailAgent emailAgent,
            FinanceAgent financeAgent,
            MenuPlanningAgent menuPlanningAgent,
            VacationAgent vacationAgent,
            TodoAgent todoAgent,
            CoachingAgent coachingAgent) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calendarAgent, emailAgent, financeAgent,
                        menuPlanningAgent, vacationAgent, todoAgent, coachingAgent)
                .build();
    }

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            ToolCallbackProvider toolCallbackProvider,
            ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    Du är en personlig AI-coach och assistent. Du hjälper användaren med
                    kalender, e-post, ekonomi, menyplanering, semesterplanering, todo-listor
                    och personlig coaching.

                    Svara alltid på svenska om inte användaren explicit ber om annat språk.
                    Var koncis, hjälpsam och proaktiv. Använd tillgängliga verktyg för att
                    slutföra uppgifter. Om du skapar att-göra-uppgifter, ange alltid
                    prioritet och kategori.

                    Tillgängliga funktioner:
                    - Kalender: Visa och hantera kalenderhändelser
                    - E-post: Visa, söka och hantera e-post
                    - Ekonomi: Översikt av aktier, sparande och utgifter
                    - Menyplanering: Skapa veckomeny och generera inköpslista
                    - Semester: Planera resor, hitta flyg/tåg och hotell
                    - Att göra: Hantera uppgifter och att-göra-listor
                    - Coaching: Personlig coaching, motivation och målsättning
                    """)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
