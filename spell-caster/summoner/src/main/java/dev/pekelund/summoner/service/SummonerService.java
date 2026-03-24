package dev.pekelund.summoner.service;

import dev.pekelund.summoner.annotation.CooldownProtected;
import dev.pekelund.summoner.model.AgentCard;
import dev.pekelund.summoner.model.ExecuteRequest;
import dev.pekelund.summoner.model.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * The Summoner Service — Orchestrator with LLM-based routing.
 *
 * Responsibilities:
 * 1. Use Gemini LLM to decide which familiar to invoke for a given request
 * 2. Enforce cooldown governance via @CooldownProtected (intercepted by CooldownAspect)
 * 3. Maintain short-term memory via MessageChatMemoryAdvisor
 * 4. Route requests to the appropriate familiar's REST endpoint
 */
@Service
public class SummonerService {

    private static final Logger log = LoggerFactory.getLogger(SummonerService.class);

    private final ChatClient chatClient;
    private final AgentDiscoveryService agentDiscoveryService;
    private final RestClient restClient;

    public SummonerService(
            ChatClient chatClient,
            AgentDiscoveryService agentDiscoveryService,
            RestClient restClient) {
        this.chatClient = chatClient;
        this.agentDiscoveryService = agentDiscoveryService;
        this.restClient = restClient;
    }

    /**
     * Main entry point: the Summoner receives a user command and routes to the right familiar.
     * Uses Gemini LLM to determine intent and target familiar.
     *
     * @param command   the user's spell-casting command
     * @param sessionId conversation session ID for memory
     * @return the familiar's response
     */
    public String summon(String command, String sessionId) {
        log.info("🔮 Summoner received command: '{}' (session: {})", command, sessionId);

        List<AgentCard> agents = agentDiscoveryService.getDiscoveredAgents();

        String agentDescriptions = agents.stream()
            .map(a -> "- " + a.name() + ": " + a.description())
            .collect(java.util.stream.Collectors.joining("\n"));

        String systemPrompt = """
                You are The Summoner, a powerful orchestrator managing three elemental familiars.
                Your job is to analyze the user's command and decide which familiar to invoke.
                
                Available familiars:
                %s
                
                Rules:
                - Respond with ONLY the familiar name (fire-familiar, water-familiar, or earth-familiar) followed by a pipe | and the target.
                - Example: fire-familiar|dragon
                - If you cannot determine which familiar to use, default to fire-familiar|unknown
                - Do NOT summon the same familiar twice in a row (check memory).
                """.formatted(agentDescriptions);

        String routing = chatClient.prompt()
            .system(systemPrompt)
            .user(command)
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", sessionId))
            .call()
            .content();

        log.info("🔮 LLM routing decision: '{}'", routing);

        if (routing == null || !routing.contains("|")) {
            return "⚠️ Summoner could not determine routing. Command: " + command;
        }

        String[] parts = routing.trim().split("\\|", 2);
        String familiarName = parts[0].trim();
        String target = parts.length > 1 ? parts[1].trim() : "unknown";

        return invokeFamiliar(familiarName, target, agents);
    }

    /**
     * Invokes the specified familiar by name.
     * The @CooldownProtected annotation is on the specific familiar methods below.
     */
    private String invokeFamiliar(String familiarName, String target, List<AgentCard> agents) {
        AgentCard card = agents.stream()
            .filter(a -> a.name().equals(familiarName))
            .findFirst()
            .orElse(null);

        if (card == null) {
            return "⚠️ Familiar '" + familiarName + "' not found among discovered agents.";
        }

        return switch (familiarName) {
            case "fire-familiar" -> invokeFire(card.endpoint(), target);
            case "water-familiar" -> invokeWater(card.endpoint(), target);
            case "earth-familiar" -> invokeEarth(card.endpoint(), target);
            default -> "⚠️ Unknown familiar: " + familiarName;
        };
    }

    @CooldownProtected(familiarName = "fire-familiar")
    public String invokeFire(String endpoint, String target) {
        return callFamiliar(endpoint, target);
    }

    @CooldownProtected(familiarName = "water-familiar")
    public String invokeWater(String endpoint, String target) {
        return callFamiliar(endpoint, target);
    }

    @CooldownProtected(familiarName = "earth-familiar")
    public String invokeEarth(String endpoint, String target) {
        return callFamiliar(endpoint, target);
    }

    @SuppressWarnings("unchecked")
    private String callFamiliar(String endpoint, String target) {
        log.info("📡 Calling familiar at {} with target: {}", endpoint, target);
        try {
            Map<String, String> response = restClient.post()
                .uri(endpoint)
                .body(new ExecuteRequest(target))
                .retrieve()
                .body(Map.class);

            if (response == null) return "⚠️ Familiar returned no response.";
            return response.getOrDefault("result", "⚠️ No result in response.");
        } catch (Exception e) {
            log.error("❌ Failed to contact familiar at {}: {}", endpoint, e.getMessage());
            return "⚠️ Failed to contact familiar at " + endpoint + ": " + e.getMessage();
        }
    }
}
