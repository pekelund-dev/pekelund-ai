package dev.pekelund.agent.controller;

import dev.pekelund.agent.dto.AnalysisResult;
import dev.pekelund.agent.dto.CreateIncidentRequest;
import dev.pekelund.agent.dto.IncidentResponse;
import dev.pekelund.agent.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Demo controller that creates and analyses pre-built incident scenarios.
 *
 * <p>This controller exists purely to make the system easy to demonstrate without
 * needing curl commands. The {@code /api/demo/simulate} endpoint creates a
 * realistic incident, runs the full AI analysis pipeline and returns the result.
 *
 * <p>Available scenarios:
 * <ul>
 *   <li>{@code payment-db}   — payment-service database connection exhaustion</li>
 *   <li>{@code fraud-memory} — fraud-service out-of-memory after deployment</li>
 *   <li>{@code gateway-down} — API gateway returning 503 for all requests</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final IncidentService incidentService;

    /** All demo scenario descriptors. */
    private static final List<DemoScenario> SCENARIOS = List.of(
            new DemoScenario(
                    "payment-db",
                    "Payment Service: Customers unable to complete purchases",
                    """
                    Users are reporting that checkout is failing with error 503.
                    The payment page shows "Service temporarily unavailable".
                    Started approximately 5 minutes ago. High volume of complaints on support chat.
                    Affects all payment methods (card, PayPal, Klarna).
                    Our monitoring shows error rate at payment-service is >80%.
                    """,
                    "monitoring-bot"
            ),
            new DemoScenario(
                    "fraud-memory",
                    "Fraud Service: Repeatedly crashing since deployment at 09:00",
                    """
                    The fraud-service pod has restarted 6 times since the deployment of v2.4.1 at 09:00.
                    Each restart happens approximately 90 minutes after startup.
                    The Java process terminates with exit code 137 (OOM kill).
                    Payment processing is degraded because fraud checks are failing open.
                    """,
                    "ops-team"
            ),
            new DemoScenario(
                    "gateway-down",
                    "API Gateway: All endpoints returning 503 or 429",
                    """
                    The API gateway started returning HTTP 429 for virtually all requests at 16:00.
                    No service deployments were done today.
                    Infrastructure team made a "minor configuration update" at 15:55.
                    Revenue impact: complete checkout failure, approximately 100% of traffic affected.
                    """,
                    "noc-alert"
            )
    );

    /**
     * Creates a pre-built incident scenario and immediately runs the full AI analysis.
     *
     * <p>This is the quickest way to see the system in action. The response includes
     * the complete analysis result from all three AI agents.
     *
     * @param scenario one of: {@code payment-db}, {@code fraud-memory}, {@code gateway-down}
     * @return the complete analysis result, including root cause, recommendations and notes
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(
            @RequestParam(defaultValue = "payment-db") String scenario) {

        DemoScenario demo = SCENARIOS.stream()
                .filter(s -> s.id().equals(scenario))
                .findFirst()
                .orElse(null);

        if (demo == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown scenario: " + scenario,
                    "available", SCENARIOS.stream().map(DemoScenario::id).toList()
            ));
        }

        log.info("Demo: simulating scenario '{}'", scenario);

        // Create the incident
        var incident = incidentService.createIncident(
                new CreateIncidentRequest(demo.title(), demo.description(), demo.reportedBy()));

        // Run the full AI analysis pipeline
        AnalysisResult result = incidentService.analyse(incident.getId());

        // Fetch the full incident with notes for the response
        IncidentResponse response = IncidentResponse.from(
                incidentService.getIncident(incident.getId()),
                incidentService.getNotes(incident.getId()));

        return ResponseEntity.ok(Map.of(
                "scenario",  scenario,
                "analysis",  result,
                "incident",  response
        ));
    }

    /**
     * Lists available demo scenarios.
     */
    @PostMapping("/scenarios")
    public List<Map<String, String>> listScenarios() {
        return SCENARIOS.stream()
                .map(s -> Map.of("id", s.id(), "title", s.title()))
                .toList();
    }

    /** Immutable descriptor for a demo incident scenario. */
    private record DemoScenario(String id, String title, String description, String reportedBy) {}
}
