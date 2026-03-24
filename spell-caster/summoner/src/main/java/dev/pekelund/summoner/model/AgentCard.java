package dev.pekelund.summoner.model;

import java.util.List;

/**
 * Agent Card — describes a familiar's identity, capabilities, and endpoint.
 * Returned by each familiar's GET /agent-card endpoint.
 * The Summoner fetches these at startup for service discovery.
 */
public record AgentCard(
    String name,
    String displayName,
    String description,
    String endpoint,
    List<AgentSkill> skills
) {}
