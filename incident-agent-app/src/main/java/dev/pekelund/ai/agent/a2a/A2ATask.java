package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * An A2A task — the central object exchanged between A2A clients and agents.
 *
 * <p>Clients send a {@code tasks/send} JSON-RPC request that includes a task ID and
 * an initial user message. The agent processes the task and returns it with a
 * completed {@link A2ATaskStatus} and agent-authored response message.
 *
 * <p>The {@code metadata} map allows clients and agents to exchange arbitrary structured
 * data alongside the text message. ITII uses it to pass:
 * <ul>
 *   <li>{@code skillId} — which skill to invoke ({@code incident_triage} or
 *       {@code incident_analysis})</li>
 *   <li>{@code incidentId} — the created incident's database ID (on output)</li>
 * </ul>
 *
 * @param id       client-supplied UUID that uniquely identifies this task
 * @param status   current task status including state and agent response message
 * @param metadata arbitrary metadata (skill routing, result IDs, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2ATask(
        String id,
        A2ATaskStatus status,
        Map<String, Object> metadata
) {}
