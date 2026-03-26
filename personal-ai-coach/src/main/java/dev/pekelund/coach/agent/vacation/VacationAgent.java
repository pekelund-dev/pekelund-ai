package dev.pekelund.coach.agent.vacation;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Semesterplaneringsagent — verktyg för att planera resor och semester.
 *
 * <p>Hjälper användaren att hitta flyg, tåg, hotell och skapa resplaner.
 * I denna initiala version ger agenten simulerade resedata.
 */
@Component
public class VacationAgent {

    @Tool(description = "Sök flyg/tåg. Parametrar: from (avresestad), to (destination), date (datum YYYY-MM-DD), passengers (antal resenärer)")
    public String searchTransport(String from, String to, String date, int passengers) {
        return String.format("""
                ✈️ Resor %s → %s den %s (%d resenärer):

                FLYG:
                1. SAS SK1234    06:30-08:45   1 295 kr/person  ⭐ Bäst pris
                2. Norwegian DY456  09:00-11:15   1 495 kr/person
                3. SAS SK1238    14:00-16:15   1 695 kr/person

                TÅG:
                1. SJ Snabbtåg   07:00-12:30   895 kr/person   🌿 Miljövänligt
                2. SJ Regional   08:30-14:00   595 kr/person   💰 Billigast
                3. SJ Snabbtåg   10:00-15:30   995 kr/person

                💡 Tips: Tåg är billigare och miljövänligare för denna sträcka!
                """, from, to, date, passengers);
    }

    @Tool(description = "Sök hotell. Parametrar: destination (stad), checkIn (datum YYYY-MM-DD), checkOut (datum YYYY-MM-DD), guests (antal gäster)")
    public String searchHotels(String destination, String checkIn, String checkOut, int guests) {
        return String.format("""
                🏨 Hotell i %s (%s till %s, %d gäster):

                1. ⭐⭐⭐⭐ Hotel Continental
                   📍 Centralt läge, 200m till tågstation
                   💰 1 890 kr/natt — Frukost inkluderad
                   ⭐ 8.7/10 (423 recensioner)

                2. ⭐⭐⭐ City Hotel Budget
                   📍 10 min promenad från centrum
                   💰 990 kr/natt — Frukost +150 kr
                   ⭐ 7.2/10 (187 recensioner)    💡 Bäst pris!

                3. ⭐⭐⭐⭐⭐ Grand Hotel Premium
                   📍 Centrum, havsutsikt
                   💰 2 890 kr/natt — All inclusive
                   ⭐ 9.4/10 (856 recensioner)    🏆 Bäst betyg

                4. 🏠 Airbnb — Lägenhet centralt
                   📍 Mysig 2:a i gamla stan
                   💰 1 200 kr/natt
                   ⭐ 9.1/10 (64 recensioner)
                """, destination, checkIn, checkOut, guests);
    }

    @Tool(description = "Skapa en resplan. Parametrar: destination (resmål), days (antal dagar)")
    public String createTravelPlan(String destination, int days) {
        return String.format("""
                🗺️ Resplan: %s (%d dagar)

                Dag 1 — Ankomst & Utforska
                • Check-in på hotellet
                • Promenad i gamla stan
                • Middag på lokal restaurang

                Dag 2 — Sightseeing
                • Besök de mest populära sevärdheterna
                • Lunch på lokal marknad
                • Kvällsunderhållning

                %s

                Beräknad totalkostnad (2 personer):
                • Transport: ~2 600 kr
                • Boende (%d nätter): ~%d kr
                • Mat & Aktiviteter: ~%d kr
                • Totalt: ~%d kr

                Vill du boka något av detta?""",
                destination, days,
                days > 2 ? "Dag 3-" + days + " — Ytterligare upplevelser och avresa\n" : "",
                days - 1, (days - 1) * 1500, days * 800,
                2600 + (days - 1) * 1500 + days * 800);
    }
}
