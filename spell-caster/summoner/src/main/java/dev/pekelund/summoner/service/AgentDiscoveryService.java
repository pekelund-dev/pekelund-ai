package dev.pekelund.summoner.service;

import dev.pekelund.summoner.model.AgentCard;
import dev.pekelund.summoner.model.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for dynamic service discovery of familiars.
 *
 * At application startup, fetches Agent Cards from each configured familiar endpoint.
 * These cards describe each familiar's capabilities and are used to register
 * dynamic tools in the Summoner's AI pipeline.
 */
@Service
public class AgentDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(AgentDiscoveryService.class);

    private final RestClient restClient;
    private final List<String> familiarBaseUrls;
    private final List<AgentCard> discoveredAgents = new ArrayList<>();

    public AgentDiscoveryService(
            RestClient restClient,
            @Value("${summoner.familiars.fire-url}") String fireUrl,
            @Value("${summoner.familiars.water-url}") String waterUrl,
            @Value("${summoner.familiars.earth-url}") String earthUrl) {
        this.restClient = restClient;
        this.familiarBaseUrls = List.of(fireUrl, waterUrl, earthUrl);
    }

    @PostConstruct
    public void discoverFamiliars() {
        log.info("🔮 Summoner initiating familiar discovery...");
        for (String baseUrl : familiarBaseUrls) {
            try {
                AgentCard card = fetchAgentCard(baseUrl);
                if (card != null) {
                    discoveredAgents.add(card);
                    log.info("✅ Discovered familiar: {} at {} with skills: {}",
                             card.displayName(), card.endpoint(),
                             card.skills().stream().map(AgentSkill::name).toList());
                }
            } catch (RestClientException e) {
                log.warn("⚠️ Could not reach familiar at {}. It may not be running. Error: {}",
                         baseUrl, e.getMessage());
            }
        }
        log.info("🔮 Discovery complete. {} familiar(s) available.", discoveredAgents.size());
    }

    @SuppressWarnings("unchecked")
    private AgentCard fetchAgentCard(String baseUrl) {
        Map<String, Object> response = restClient.get()
            .uri(baseUrl + "/agent-card")
            .retrieve()
            .body(Map.class);

        if (response == null) return null;

        String name = (String) response.get("name");
        String displayName = (String) response.get("displayName");
        String description = (String) response.get("description");
        String endpoint = (String) response.get("endpoint");

        List<AgentSkill> skills = new ArrayList<>();
        Object skillsObj = response.get("skills");
        if (skillsObj instanceof List<?> skillsList) {
            for (Object skillObj : skillsList) {
                if (skillObj instanceof Map<?, ?> skillMap) {
                    skills.add(new AgentSkill(
                        (String) skillMap.get("name"),
                        (String) skillMap.get("description")
                    ));
                }
            }
        }

        return new AgentCard(name, displayName, description, endpoint, skills);
    }

    /**
     * Returns the list of discovered agent cards.
     */
    public List<AgentCard> getDiscoveredAgents() {
        return Collections.unmodifiableList(discoveredAgents);
    }
}
