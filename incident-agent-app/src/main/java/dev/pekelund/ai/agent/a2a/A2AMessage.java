package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * An A2A message — a turn in a conversation between client and agent.
 *
 * <p>Messages are the unit of communication in A2A. A task's input is a user message;
 * the agent's output is an agent message. Each message contains one or more
 * {@link A2APart parts} that carry the actual content.
 *
 * @param role   {@code "user"} (client's input) or {@code "agent"} (agent's output)
 * @param parts  ordered list of content parts composing this message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2AMessage(
        String role,
        List<A2APart> parts
) {

    /**
     * A single content part within a message.
     *
     * <p>The A2A spec supports several part types. This implementation supports
     * {@code text/plain} text parts — sufficient for the ITII use case.
     * In a production agent you might also handle {@code application/json} data
     * parts or file attachments.
     *
     * @param type    MIME type of the part content ({@code "text/plain"})
     * @param text    the text content (present when {@code type} is {@code "text/plain"})
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record A2APart(
            String type,
            String text
    ) {
        /** Convenience factory for a plain-text part. */
        public static A2APart text(String content) {
            return new A2APart("text/plain", content);
        }
    }
}
