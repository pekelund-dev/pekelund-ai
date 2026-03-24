package dev.pekelund.familiar.fire.controller;

import dev.pekelund.familiar.fire.service.FireFamiliarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Fire Familiar.
 * Exposes Agent Card discovery and execution endpoints.
 */
@RestController
public class FamiliarController {

    private final FireFamiliarService fireFamiliarService;

    public FamiliarController(FireFamiliarService fireFamiliarService) {
        this.fireFamiliarService = fireFamiliarService;
    }

    /**
     * Agent Card endpoint — describes this familiar's capabilities.
     * The Summoner calls this at startup to discover what this familiar can do.
     */
    @GetMapping("/agent-card")
    public ResponseEntity<Map<String, Object>> agentCard() {
        return ResponseEntity.ok(Map.of(
            "name", "fire-familiar",
            "displayName", "Ignis — Fire Familiar",
            "description", "Sequential fire agent that scouts the Librarium database for a spell and amplifies it. Uses a linear Scout → Amplify execution chain.",
            "endpoint", "http://localhost:8091/execute",
            "skills", List.of(Map.of(
                "name", "fire_attack",
                "description", "Scout the Librarium for a fire spell and amplify it with Ignis Amplification Protocol. Input: element name or attack target."
            ))
        ));
    }

    /**
     * Execute endpoint — triggers the fire familiar's sequential attack.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> execute(@RequestBody Map<String, String> request) {
        String target = request.getOrDefault("target", "fire");
        String result = fireFamiliarService.execute(target);
        return ResponseEntity.ok(Map.of(
            "familiar", "fire",
            "pattern", "sequential",
            "result", result
        ));
    }
}
