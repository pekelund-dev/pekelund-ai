package dev.pekelund.ai.agent.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON-RPC 2.0 response envelope returned by A2A task operations.
 *
 * <p>Exactly one of {@link #result} or {@link #error} will be non-null:
 * <ul>
 *   <li>{@link #result} — the successful response (an {@link A2ATask} for task methods)</li>
 *   <li>{@link #error}  — the error object for failed or unrecognised requests</li>
 * </ul>
 *
 * @param jsonrpc protocol version — always {@code "2.0"}
 * @param id      correlation ID echoed from the request
 * @param result  success payload (mutually exclusive with {@link #error})
 * @param error   error payload (mutually exclusive with {@link #result})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record A2AJsonRpcResponse(
        String jsonrpc,
        String id,
        Object result,
        A2AError error
) {

    /** Standard JSON-RPC 2.0 error codes defined by the A2A specification. */
    public enum ErrorCode {
        PARSE_ERROR(-32700),
        INVALID_REQUEST(-32600),
        METHOD_NOT_FOUND(-32601),
        INVALID_PARAMS(-32602),
        INTERNAL_ERROR(-32603),
        TASK_NOT_FOUND(-32001),
        TASK_CANNOT_BE_CANCELLED(-32002);

        public final int code;
        ErrorCode(int code) { this.code = code; }
    }

    /**
     * A JSON-RPC 2.0 error object.
     *
     * @param code    standard error code (see {@link ErrorCode})
     * @param message human-readable error description
     */
    public record A2AError(int code, String message) {}

    // ── Factory Methods ────────────────────────────────────────────────────────

    /** Creates a successful response. */
    public static A2AJsonRpcResponse success(String requestId, Object result) {
        return new A2AJsonRpcResponse("2.0", requestId, result, null);
    }

    /** Creates an error response with a standard {@link ErrorCode}. */
    public static A2AJsonRpcResponse error(String requestId, ErrorCode code, String message) {
        return new A2AJsonRpcResponse("2.0", requestId, null, new A2AError(code.code, message));
    }
}
