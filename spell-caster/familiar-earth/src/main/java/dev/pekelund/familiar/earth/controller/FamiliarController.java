package dev.pekelund.familiar.earth.controller;

import dev.pekelund.familiar.earth.service.EarthFamiliarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Earth Familiar.
 */
@RestController
public class FamiliarController {

    private final EarthFamiliarService earthFamiliarService;

    public FamiliarController(EarthFamiliarService earthFamiliarService) {
        this.earthFamiliarService = earthFamiliarService;
    }

    @GetMapping("/agent-card")
    public ResponseEntity<Map<String, Object>> agentCard() {
        return ResponseEntity.ok(Map.of(
            "name", "earth-familiar",
            "displayName", "Terra — Earth Familiar",
            "description", "Iterative earth agent that repeatedly calls seismic_charge to accumulate energy. Releases attack when energy threshold (100) is reached or max iterations (10) exceeded.",
            "endpoint", "http://localhost:8093/execute",
            "skills", List.of(Map.of(
                "name", "earth_attack",
                "description", "Iteratively accumulate seismic energy via seismic_charge until the 100-energy threshold is reached (max 10 iterations), then release the attack. Input: attack target."
            ))
        ));
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> execute(@RequestBody Map<String, String> request) {
        String target = request.getOrDefault("target", "ground");
        String result = earthFamiliarService.execute(target);
        return ResponseEntity.ok(Map.of(
            "familiar", "earth",
            "pattern", "iterative",
            "result", result
        ));
    }
}
