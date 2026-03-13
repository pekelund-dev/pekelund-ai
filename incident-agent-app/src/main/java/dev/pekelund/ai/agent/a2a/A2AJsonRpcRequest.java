package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 request envelope for A2A task operations.
 *
 * <p>A2A uses JSON-RPC 2.0 as its wire format over HTTP POST.
 * The {@link #method} field determines the operation:
 * <ul>
 *   <li>{@code tasks/send}   — submit a new task (params: id, message, skillId, metadata)</li>
 *   <li>{@code tasks/get}    — retrieve a task by ID (params: id)</li>
 *   <li>{@code tasks/cancel} — cancel a running task (params: id)</li>
 * </ul>
 *
 * <p>The {@link #params} is an untyped {@link JsonNode} so each method handler can
 * deserialise only the fields it needs without requiring separate request classes.
 *
 * @param jsonrpc protocol version — must always be {@code "2.0"}
 * @param id      correlation ID (string or number); echoed back in the response
 * @param method  method name (e.g. {@code "tasks/send"})
 * @param params  method-specific parameters as raw JSON
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2AJsonRpcRequest(
        String jsonrpc,
        String id,
        String method,
        JsonNode params
) {}
