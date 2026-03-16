package dev.pekelund.personal.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
            ? request.conversationId()
            : UUID.randomUUID().toString();

        String response = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        return new ChatResponse(response, conversationId);
    }

    public record ChatRequest(String message, String conversationId) {}
    public record ChatResponse(String response, String conversationId) {}
}
