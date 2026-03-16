package dev.pekelund.personal.controller;

import dev.pekelund.personal.agent.AgentService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
            ? request.conversationId()
            : UUID.randomUUID().toString();

        AgentService.ChatResult result = agentService.chat(request.message(), conversationId);

        return new ChatResponse(result.content(), conversationId, result.modelUsed());
    }

    public record ChatRequest(String message, String conversationId) {}
    public record ChatResponse(String response, String conversationId, String modelUsed) {}
}
