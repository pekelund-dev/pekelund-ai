package dev.pekelund.coach.agent.coaching;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Coachingagent — verktyg för personlig coaching och motivation.
 *
 * <p>Hjälper användaren med målsättning, vanor, motivation och
 * personlig utveckling.
 */
@Component
public class CoachingAgent {

    private static final Locale SV = Locale.of("sv", "SE");
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Tool(description = "Visa en daglig motivationssammanfattning med mål och framsteg.")
    public String getDailyMotivation() {
        String today = LocalDate.now(STOCKHOLM).format(DateTimeFormatter.ofPattern("d MMMM yyyy", SV));
        return String.format("""
                🎯 God morgon! Här är din dag den %s:

                📊 Dina aktiva mål:
                1. 🏃 Träna 3 gånger/vecka — Framsteg: 2/3 ✅ (nästan där!)
                2. 📚 Läsa 30 min/dag — Framsteg: 5/7 dagar ✅
                3. 💰 Spara 5 000 kr/mån — Framsteg: 3 200 kr (64%%)
                4. 🧘 Meditera dagligen — Framsteg: 4/7 dagar

                💪 Dagens tips:
                "Framgång är summan av små ansträngningar, upprepade dag efter dag."
                — Robert Collier

                Vad vill du fokusera på idag?""", today);
    }

    @Tool(description = "Skapa ett nytt mål. Parametrar: goal (målet), deadline (datum YYYY-MM-DD), milestones (delmål separerade med komma)")
    public String createGoal(String goal, String deadline, String milestones) {
        LocalDate deadlineDate = LocalDate.parse(deadline);
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(STOCKHOLM), deadlineDate);
        return String.format("""
                🎯 Nytt mål skapat!

                Mål: %s
                Deadline: %s (%d dagar kvar)

                Delmål:
                %s

                Tips: Bryt ner ditt mål i dagliga handlingar för bäst resultat.
                Vill du att jag skapar en plan för att nå detta mål?""",
                goal,
                deadlineDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", SV)),
                daysLeft,
                formatMilestones(milestones));
    }

    @Tool(description = "Visa veckosammanfattning av framsteg mot mål.")
    public String getWeeklyProgress() {
        return """
                📊 Veckosammanfattning:

                🏆 Bra jobbat denna vecka!

                ✅ Uppnått:
                • Tränade 3 gånger (mål uppnått! 🎉)
                • Läste 4 av 7 dagar
                • Sparade 1 200 kr

                📈 Trender:
                • Träning: ↑ Bättre än förra veckan
                • Läsning: ↓ Lite sämre, men fortfarande bra
                • Sparande: → Stabilt

                💡 Fokusområde nästa vecka:
                Försök att läsa lite varje dag — kanske 15 min vid läggdags?

                Poäng denna vecka: 78/100 ⭐⭐⭐⭐
                """;
    }

    @Tool(description = "Ge coaching-råd baserat på ett område. Parameter: topic (ämne, t.ex. 'motivation', 'produktivitet', 'hälsa', 'ekonomi')")
    public String getCoachingAdvice(String topic) {
        return String.format("""
                🎯 Coaching: %s

                Här är tre konkreta tips:

                1. Sätt upp specifika och mätbara mål
                   → Istället för "träna mer", säg "springa 5 km 3 gånger/vecka"

                2. Skapa rutiner och vanor
                   → Koppla nya vanor till befintliga (efter frukost → 10 min meditation)

                3. Följ upp och fira framsteg
                   → Skriv ner dina framsteg varje söndag och fira små vinster

                Vill du att jag hjälper dig skapa en konkret plan för %s?""", topic, topic);
    }

    private String formatMilestones(String milestones) {
        if (milestones == null || milestones.isBlank()) return "• (inga delmål angivna)";
        StringBuilder sb = new StringBuilder();
        for (String milestone : milestones.split(",")) {
            sb.append("• □ ").append(milestone.trim()).append("\n");
        }
        return sb.toString().trim();
    }
}
