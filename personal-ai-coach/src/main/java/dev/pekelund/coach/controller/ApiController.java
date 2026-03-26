package dev.pekelund.coach.controller;

import dev.pekelund.coach.agent.AgentService;
import dev.pekelund.coach.domain.AgentType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for programmatic access to the AI Coach.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AgentService agentService;

    public ApiController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        AgentType agentType = request.agentType() != null
                ? request.agentType()
                : AgentType.GENERAL;

        AgentService.ChatResult result = agentService.chat(
                request.message(), conversationId, agentType);

        return new ChatResponse(result.content(), conversationId,
                result.modelUsed(), agentType);
    }

    public record ChatRequest(String message, String conversationId, AgentType agentType) {}
    public record ChatResponse(String response, String conversationId,
                               String modelUsed, AgentType agentType) {}
}
