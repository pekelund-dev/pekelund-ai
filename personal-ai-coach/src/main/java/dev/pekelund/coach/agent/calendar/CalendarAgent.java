package dev.pekelund.coach.agent.calendar;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Kalenderagent — verktyg för att hantera kalenderhändelser.
 *
 * <p>I denna initiala version ger agenten simulerade kalenderhändelser.
 * I produktion integrerar den med Google Calendar API.
 */
@Component
public class CalendarAgent {

    private static final Locale SV = Locale.of("sv", "SE");
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Tool(description = "Visa dagens kalenderhändelser. Visar alla möten och händelser för idag.")
    public String getTodaysEvents() {
        LocalDate today = LocalDate.now(STOCKHOLM);
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, SV);
        return String.format("""
                📅 Kalender för %s %s:

                09:00 - 09:30  Morgonmöte med teamet
                10:00 - 11:00  Projektgenomgång Q2
                12:00 - 13:00  Lunch
                14:00 - 15:00  Kundpresentation
                16:00 - 16:30  Veckoavstämning

                Totalt: 5 händelser idag.""", dayName, today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", SV)));
    }

    @Tool(description = "Visa veckans kalenderhändelser. Parameter: weekOffset (0 för denna vecka, 1 för nästa vecka)")
    public String getWeekEvents(int weekOffset) {
        LocalDate startOfWeek = LocalDate.now(STOCKHOLM)
                .plusWeeks(weekOffset)
                .with(java.time.DayOfWeek.MONDAY);
        return String.format("""
                📅 Kalenderöversikt vecka %d (%s - %s):

                Måndag:    3 händelser (inkl. Sprintplanering 09:00)
                Tisdag:    2 händelser
                Onsdag:    4 händelser (fullbokad förmiddag)
                Torsdag:   2 händelser
                Fredag:    1 händelse (Fredagsfika 15:00)

                Totalt: 12 händelser denna vecka.""",
                startOfWeek.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()),
                startOfWeek.format(DateTimeFormatter.ofPattern("d MMM", SV)),
                startOfWeek.plusDays(4).format(DateTimeFormatter.ofPattern("d MMM", SV)));
    }

    @Tool(description = "Skapa en ny kalenderhändelse. Parametrar: title (rubrik), date (datum YYYY-MM-DD), time (tid HH:mm), durationMinutes (längd i minuter)")
    public String createEvent(String title, String date, String time, int durationMinutes) {
        LocalDateTime start = LocalDateTime.parse(date + "T" + time);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        return String.format("✅ Händelse skapad: '%s' den %s kl %s-%s",
                title,
                start.format(DateTimeFormatter.ofPattern("d MMMM", SV)),
                start.format(DateTimeFormatter.ofPattern("HH:mm")),
                end.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    @Tool(description = "Hämta aktuellt datum och tid i Sverige.")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now(STOCKHOLM);
        String dayName = now.getDayOfWeek().getDisplayName(TextStyle.FULL, SV);
        return String.format("🕐 Just nu: %s %s",
                dayName,
                now.format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", SV)));
    }
}
