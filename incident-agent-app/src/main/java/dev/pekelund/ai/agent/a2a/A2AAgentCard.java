package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A2A Agent Card — published at {@code GET /.well-known/agent.json}.
 *
 * <p>The <b>Agent Card</b> is the discovery document of the
 * <a href="https://a2a-protocol.org">Agent-to-Agent (A2A) Protocol</a>.
 * Any A2A-compatible client that finds this endpoint can:
 * <ol>
 *   <li>Learn what skills the agent offers</li>
 *   <li>Understand what input/output modalities are supported</li>
 *   <li>Invoke the agent's task endpoint with a conformant request</li>
 * </ol>
 *
 * <h2>A2A in a Nutshell</h2>
 * <p>A2A is an open standard (by Google, 2025) that defines how AI agents discover each
 * other and exchange work. It uses plain HTTP + JSON-RPC 2.0 — deliberately simple so any
 * language or framework can implement it. Key ideas:
 * <ul>
 *   <li>Every agent publishes this agent card at a well-known URL</li>
 *   <li>Clients POST JSON-RPC {@code tasks/send} requests to the agent's {@code url}</li>
 *   <li>Agents return structured {@link A2ATask} responses with parts (text, data, file)</li>
 * </ul>
 *
 * @param name               human-readable agent name
 * @param description        what this agent does
 * @param url                task endpoint — clients POST JSON-RPC messages here
 * @param version            agent version string
 * @param capabilities       optional protocol capabilities (streaming, push notifications)
 * @param skills             list of named skills this agent can perform
 * @param defaultInputModes  supported input content types (default: ["text/plain"])
 * @param defaultOutputModes supported output content types (default: ["text/plain"])
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2AAgentCard(
        String name,
        String description,
        String url,
        String version,
        A2ACapabilities capabilities,
        List<A2ASkill> skills,
        List<String> defaultInputModes,
        List<String> defaultOutputModes
) {

    /**
     * Agent capabilities — which optional A2A features this agent supports.
     *
     * @param streaming         whether the agent can stream incremental updates via SSE
     * @param pushNotifications whether the agent can push status updates to a webhook
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record A2ACapabilities(
            boolean streaming,
            boolean pushNotifications
    ) {}
}
