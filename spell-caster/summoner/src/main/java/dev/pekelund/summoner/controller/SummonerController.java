package dev.pekelund.summoner.controller;

import dev.pekelund.summoner.model.AgentCard;
import dev.pekelund.summoner.service.AgentDiscoveryService;
import dev.pekelund.summoner.service.SummonerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for The Summoner orchestrator.
 */
@RestController
@RequestMapping("/summon")
public class SummonerController {

    private final SummonerService summonerService;
    private final AgentDiscoveryService agentDiscoveryService;

    public SummonerController(SummonerService summonerService, AgentDiscoveryService agentDiscoveryService) {
        this.summonerService = summonerService;
        this.agentDiscoveryService = agentDiscoveryService;
    }

    /**
     * Main summoning endpoint. Routes the command to the appropriate familiar.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> summon(@RequestBody Map<String, String> request) {
        String command = request.getOrDefault("command", "");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());

        String result = summonerService.summon(command, sessionId);
        return ResponseEntity.ok(Map.of(
            "command", command,
            "sessionId", sessionId,
            "result", result
        ));
    }

    /**
     * Lists all currently discovered familiars and their agent cards.
     */
    @GetMapping("/familiars")
    public ResponseEntity<List<AgentCard>> listFamiliars() {
        return ResponseEntity.ok(agentDiscoveryService.getDiscoveredAgents());
    }
}
