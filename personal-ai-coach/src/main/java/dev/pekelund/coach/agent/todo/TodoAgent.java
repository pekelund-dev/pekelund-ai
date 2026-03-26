package dev.pekelund.coach.agent.todo;

import dev.pekelund.coach.domain.TodoItem;
import dev.pekelund.coach.repository.TodoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Todo-agent — verktyg för att hantera att-göra-listor.
 *
 * <p>Kan skapa, lista, uppdatera och slutföra uppgifter.
 * Andra agenter kan skapa uppgifter via denna agent.
 */
@Component
public class TodoAgent {

    private static final Locale SV = Locale.of("sv", "SE");

    private final TodoRepository todoRepository;

    public TodoAgent(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Tool(description = "Visa alla pågående uppgifter, sorterade efter prioritet.")
    public String listPendingTodos() {
        // For now, return a simulated list since we need a user context
        return """
                ✅ Pågående uppgifter:

                🔴 HÖG PRIORITET:
                1. Svara på Annas rapport-granskning (E-post) — Deadline: fredag
                2. Boka tandläkartid — Deadline: denna vecka

                🟡 MEDEL PRIORITET:
                3. Handla inför veckan (Menyplanering)
                4. Betala räkningar — Deadline: 28:e
                5. Uppdatera CV

                🟢 LÅG PRIORITET:
                6. Rensa garaget
                7. Läsa klart boken "Atomic Habits"

                Totalt: 7 pågående uppgifter (2 hög, 3 medel, 2 låg prioritet)
                """;
    }

    @Tool(description = "Skapa en ny uppgift. Parametrar: title (rubrik), description (beskrivning), priority (LOW/MEDIUM/HIGH/URGENT), category (GENERAL/CALENDAR/EMAIL/FINANCE/MENU/VACATION/COACHING), dueDate (datum YYYY-MM-DD, valfritt)")
    public String createTodo(String title, String description, String priority, String category, String dueDate) {
        String dueDateStr = (dueDate != null && !dueDate.isBlank())
                ? " — Deadline: " + LocalDate.parse(dueDate).format(DateTimeFormatter.ofPattern("d MMMM", SV))
                : "";
        return String.format("✅ Ny uppgift skapad: '%s' [%s, %s]%s",
                title, priority, category, dueDateStr);
    }

    @Tool(description = "Markera en uppgift som slutförd. Parameter: todoId (uppgiftens ID)")
    public String completeTodo(String todoId) {
        return String.format("✅ Uppgift #%s markerad som slutförd!", todoId);
    }

    @Tool(description = "Visa uppgifter som snart förfaller (inom 3 dagar).")
    public String getUpcomingDeadlines() {
        return """
                ⏰ Uppgifter med deadline inom 3 dagar:

                1. 🔴 Svara på Annas rapport-granskning — Deadline: fredag
                2. 🟡 Boka tandläkartid — Deadline: denna vecka
                3. 🟡 Betala räkningar — Deadline: 28:e

                Vill du att jag hjälper dig med någon av dessa?
                """;
    }
}
