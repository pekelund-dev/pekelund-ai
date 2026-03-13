package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * The status of an A2A task, containing its lifecycle state and the agent's output message.
 *
 * <h2>A2A Task Lifecycle States</h2>
 * <pre>
 *   submitted → working → completed   (happy path)
 *                       ↘ failed
 *                       ↘ canceled
 *              → input-required        (agent needs clarification — not used here)
 * </pre>
 *
 * <p>This implementation uses only {@code submitted} → {@code working} → {@code completed}
 * (or {@code failed}) since tasks are processed synchronously.
 *
 * @param state     current task state string
 * @param message   agent's response message (present when state is {@code completed})
 * @param timestamp ISO-8601 timestamp of the last state transition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2ATaskStatus(
        String state,
        A2AMessage message,
        String timestamp
) {

    /** Terminal state: task completed successfully with an agent message. */
    public static A2ATaskStatus completed(A2AMessage agentMessage) {
        return new A2ATaskStatus("completed", agentMessage, Instant.now().toString());
    }

    /** Terminal state: task failed due to an error. */
    public static A2ATaskStatus failed(String errorMessage) {
        return new A2ATaskStatus("failed",
                new A2AMessage("agent", java.util.List.of(A2AMessage.A2APart.text(errorMessage))),
                Instant.now().toString());
    }

    /** Transient state: task has been received but processing has not started. */
    public static A2ATaskStatus submitted() {
        return new A2ATaskStatus("submitted", null, Instant.now().toString());
    }
}
