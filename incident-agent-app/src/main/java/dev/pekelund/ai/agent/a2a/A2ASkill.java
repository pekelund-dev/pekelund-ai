package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Describes a single skill that this A2A agent can perform.
 *
 * <p>Skills are how clients discover what an agent can do. When a client reads the
 * agent card it inspects the skills list to decide which task to send. The {@link #id}
 * is used as the {@code skillId} parameter in {@code tasks/send} requests.
 *
 * @param id           machine-readable skill identifier (used as {@code skillId} in tasks)
 * @param name         human-readable skill name
 * @param description  detailed description of what the skill does
 * @param inputModes   content types accepted as input (e.g. {@code "text/plain"})
 * @param outputModes  content types produced as output
 * @param examples     illustrative example inputs to help clients construct valid requests
 * @param tags         optional classification tags (for discovery/filtering)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2ASkill(
        String id,
        String name,
        String description,
        List<String> inputModes,
        List<String> outputModes,
        List<String> examples,
        List<String> tags
) {}
