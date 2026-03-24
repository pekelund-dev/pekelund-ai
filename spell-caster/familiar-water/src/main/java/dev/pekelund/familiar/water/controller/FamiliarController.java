package dev.pekelund.familiar.water.controller;

import dev.pekelund.familiar.water.service.WaterFamiliarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Water Familiar.
 */
@RestController
public class FamiliarController {

    private final WaterFamiliarService waterFamiliarService;

    public FamiliarController(WaterFamiliarService waterFamiliarService) {
        this.waterFamiliarService = waterFamiliarService;
    }

    @GetMapping("/agent-card")
    public ResponseEntity<Map<String, Object>> agentCard() {
        return ResponseEntity.ok(Map.of(
            "name", "water-familiar",
            "displayName", "Aqua — Water Familiar",
            "description", "Parallel water agent that uses Java StructuredTaskScope to execute Nexus (cryo_shatter) and Forge (moonlight_cascade) channels simultaneously, then merges them with Power Merger.",
            "endpoint", "http://localhost:8092/execute",
            "skills", List.of(Map.of(
                "name", "water_attack",
                "description", "Execute a parallel dual-channel water attack: cryo_shatter via Nexus + moonlight_cascade via Forge, merged by Power Merger. Input: attack target."
            ))
        ));
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> execute(@RequestBody Map<String, String> request) {
        String target = request.getOrDefault("target", "enemy");
        String result = waterFamiliarService.execute(target);
        return ResponseEntity.ok(Map.of(
            "familiar", "water",
            "pattern", "parallel",
            "result", result
        ));
    }
}
