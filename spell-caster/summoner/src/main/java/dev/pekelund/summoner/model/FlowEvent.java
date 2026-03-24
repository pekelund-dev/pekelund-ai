package dev.pekelund.summoner.model;

/**
 * Represents a single step in the Summoner's execution flow.
 * Captured during a summon call and displayed in the Web UI.
 *
 * @param type         category (COMMAND, ROUTING, SELECTED, INVOKE, COOLDOWN, RESULT, ERROR)
 * @param emoji        visual indicator shown in the UI
 * @param message      human-readable description of the step
 * @param familiarName the familiar involved (null for LLM/routing steps)
 * @param time         wall-clock time formatted as HH:mm:ss
 */
public record FlowEvent(
        String type,
        String emoji,
        String message,
        String familiarName,
        String time) {

    /** CSS class suffix used in the Thymeleaf template. */
    public String cssType() {
        return type().toLowerCase();
    }
}
