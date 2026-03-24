package dev.pekelund.summoner.controller;

import dev.pekelund.summoner.aspect.CooldownAspect;
import dev.pekelund.summoner.model.AgentCard;
import dev.pekelund.summoner.model.FamiliarStatus;
import dev.pekelund.summoner.model.FlowEvent;
import dev.pekelund.summoner.service.AgentDiscoveryService;
import dev.pekelund.summoner.service.FlowTracker;
import dev.pekelund.summoner.service.SummonerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thymeleaf + HTMX Web controller for The Summoner dashboard.
 *
 * Endpoints:
 * - GET  /           — Main dashboard page
 * - GET  /ui/familiars — HTMX fragment: sidebar familiar status (auto-refreshed every 5s)
 * - POST /ui/summon   — HTMX fragment: execute a summon + return result + OOB flow update
 */
@Controller
public class WebController {

    /**
     * Static metadata for the three known familiars.
     * Machine names must match the values used in the @CooldownProtected annotations
     * and in the REST controller /agent-card responses.
     */
    private record FamiliarDefinition(String name, String displayName, String pattern, int port) {}

    private static final List<FamiliarDefinition> FAMILIAR_DEFINITIONS = List.of(
        new FamiliarDefinition("fire-familiar",  "🔥 Ignis",  "Sequential", 8091),
        new FamiliarDefinition("water-familiar", "🌊 Aqua",   "Parallel",   8092),
        new FamiliarDefinition("earth-familiar", "🌍 Terra",  "Iterative",  8093)
    );

    private final SummonerService summonerService;
    private final AgentDiscoveryService agentDiscoveryService;
    private final CooldownAspect cooldownAspect;
    private final FlowTracker flowTracker;

    public WebController(
            SummonerService summonerService,
            AgentDiscoveryService agentDiscoveryService,
            CooldownAspect cooldownAspect,
            FlowTracker flowTracker) {
        this.summonerService = summonerService;
        this.agentDiscoveryService = agentDiscoveryService;
        this.cooldownAspect = cooldownAspect;
        this.flowTracker = flowTracker;
    }

    /** Serves the main dashboard. A new session UUID is seeded server-side,
     *  but JavaScript will override it from localStorage to persist the session
     *  across page reloads. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        model.addAttribute("familiars", buildFamiliarStatuses());
        return "index";
    }

    /**
     * HTMX fragment: returns updated familiar cards for the sidebar.
     * Triggered on page load and every 5 seconds via hx-trigger="load, every 5s".
     */
    @GetMapping("/ui/familiars")
    public String familiars(Model model) {
        model.addAttribute("familiars", buildFamiliarStatuses());
        return "fragments/familiars :: cards";
    }

    /**
     * HTMX endpoint: executes a summon command and returns an HTML fragment with:
     * - Main content for #result-area (the spell result)
     * - OOB swap for #flow-panel (execution flow steps)
     *
     * The form uses hx-target="#result-area" so the main fragment replaces the result area,
     * while HTMX processes the hx-swap-oob elements to update other parts of the page.
     */
    @PostMapping("/ui/summon")
    public String summon(
            @RequestParam String command,
            @RequestParam(defaultValue = "") String sessionId,
            Model model) {

        if (sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String result = summonerService.summon(command, sessionId);
        List<FlowEvent> flowEvents = flowTracker.getAndClear(sessionId);

        model.addAttribute("result", result);
        model.addAttribute("command", command);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("flowEvents", flowEvents);
        model.addAttribute("familiars", buildFamiliarStatuses());
        model.addAttribute("blocked", result.startsWith("⛔"));
        model.addAttribute("error", result.startsWith("⚠️"));

        return "fragments/summon-response :: result";
    }

    /**
     * Builds the list of familiar status view-models for the UI sidebar.
     *
     * Online status is derived from the discovery service (populated at startup).
     * Remaining cooldown and the configured cooldown window come from the AOP aspect
     * so the progress bar matches the actual governance rule.
     */
    private List<FamiliarStatus> buildFamiliarStatuses() {
        Set<String> onlineNames = agentDiscoveryService.getDiscoveredAgents()
                .stream()
                .map(AgentCard::name)
                .collect(Collectors.toSet());

        Map<String, Long> cooldowns = cooldownAspect.getRemainingCooldowns();

        return FAMILIAR_DEFINITIONS.stream()
                .map(def -> new FamiliarStatus(
                        def.name(),
                        def.displayName(),
                        def.pattern(),
                        def.port(),
                        onlineNames.contains(def.name()),
                        cooldowns.getOrDefault(def.name(), 0L),
                        cooldownAspect.getRegisteredCooldownSeconds(def.name())))
                .toList();
    }
}
