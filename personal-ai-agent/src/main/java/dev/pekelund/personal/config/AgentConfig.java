package dev.pekelund.personal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pekelund.personal.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AgentConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            GmailTools gmailTools,
            CalendarTools calendarTools,
            DateTimeTools dateTimeTools,
            WeatherTools weatherTools,
            NotesTools notesTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(gmailTools, calendarTools, dateTimeTools, weatherTools, notesTools)
                .build();
    }

    @Bean
    public ChatClient chatClient(
            org.springframework.ai.chat.model.ChatModel chatModel,
            ToolCallbackProvider toolCallbackProvider,
            ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are a helpful personal AI assistant for the user. You have access to their Gmail and Google Calendar.
                    You can also check the current date/time, look up weather information, and manage personal notes.
                    Always be concise, helpful, and proactive. When the user asks you to do something with their email or calendar,
                    use the available tools to complete the task.
                    Current capabilities:
                    - Gmail: list, read, search, and send emails
                    - Google Calendar: list upcoming events, create, update, and delete events
                    - Date/Time: get current date and time
                    - Weather: get current weather and forecasts
                    - Notes: save and retrieve personal notes
                    """)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
