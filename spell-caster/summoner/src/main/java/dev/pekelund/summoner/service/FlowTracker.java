package dev.pekelund.summoner.service;

import dev.pekelund.summoner.model.FlowEvent;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store that captures execution flow events per session.
 *
 * The Summoner service records each step (command received, LLM routing,
 * familiar selected, cooldown check, REST call, result) so the Web UI
 * can display a live execution trace after each summon call.
 */
@Component
public class FlowTracker {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConcurrentHashMap<String, List<FlowEvent>> sessions = new ConcurrentHashMap<>();

    /**
     * Records a flow event for the given session.
     *
     * @param sessionId    conversation / session identifier
     * @param type         event category (COMMAND, ROUTING, SELECTED, INVOKE, COOLDOWN, RESULT, ERROR)
     * @param emoji        visual indicator
     * @param message      human-readable description
     * @param familiarName the involved familiar, or null
     */
    public void track(String sessionId, String type, String emoji, String message, String familiarName) {
        if (sessionId == null) return;
        sessions.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new FlowEvent(type, emoji, message, familiarName,
                        LocalTime.now().format(TIME_FMT)));
    }

    /**
     * Returns all events for a session and removes them from the store.
     * Called after a summon completes to send events to the UI.
     */
    public List<FlowEvent> getAndClear(String sessionId) {
        List<FlowEvent> events = sessions.remove(sessionId);
        return events != null ? List.copyOf(events) : List.of();
    }
}
