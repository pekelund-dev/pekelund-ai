package dev.pekelund.coach.agent.menu;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

/**
 * Menyplaneringsagent — verktyg för veckomenyplanering och inköpslistor.
 *
 * <p>Hjälper användaren att planera veckomenyerna med hänsyn till
 * matpreferenser, allergier och budget.
 */
@Component
public class MenuPlanningAgent {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Tool(description = "Generera ett förslag på veckomeny. Parameter: preferences (matpreferenser som 'vegetariskt', 'barnvänligt', 'budget' etc.)")
    public String generateWeeklyMenu(String preferences) {
        LocalDate today = LocalDate.now(STOCKHOLM);
        String week = today.get(WeekFields.ISO.weekOfWeekBasedYear()) + "";
        return String.format("""
                🍽️ Veckomeny v.%s (anpassad för: %s):

                Måndag:    Köttfärssås med spagetti & sallad
                Tisdag:    Kycklinggryta med ris & grönsaker
                Onsdag:    Laxfilé med potatispuré & citron
                Torsdag:   Vegetarisk tacos med bönor & guacamole
                Fredag:    Fredagsmys! Pizza med valfri topping
                Lördag:    Köttbullar med potatismos & lingon
                Söndag:    Ugnsstekt kyckling med rotsaker

                Beräknad kostnad: ~1 200 kr (4 portioner/måltid)

                Vill du se recept för någon specifik dag?""", week, preferences);
    }

    @Tool(description = "Generera inköpslista baserat på veckomenyn.")
    public String generateShoppingList() {
        return """
                🛒 Inköpslista för veckomenyn:

                🥩 KÖTT & FISK:
                □ Köttfärs 500g
                □ Kycklingfilé 600g
                □ Laxfilé 4 st
                □ Hel kyckling 1 st
                □ Köttbullar 500g

                🥬 GRÖNSAKER:
                □ Sallad 1 st
                □ Tomater 6 st
                □ Paprika 3 st
                □ Lök 4 st
                □ Vitlök 1 st
                □ Avokado 3 st
                □ Rotsaker (morot, palsternacka) 1 kg
                □ Potatis 2 kg

                🧀 MEJERI:
                □ Grädde 2 dl
                □ Ost (riven) 200g
                □ Smör 1 paket

                🍞 TORRVAROR:
                □ Spagetti 500g
                □ Ris 1 kg
                □ Tacoskal 12 st
                □ Pizzadeg 2 st

                🫙 ÖVRIGT:
                □ Kokosmjölk 400ml
                □ Krossade tomater 2 burkar
                □ Lingonsylt 1 burk
                □ Citron 2 st

                Beräknad totalkostnad: ~1 200 kr
                """;
    }

    @Tool(description = "Visa recept för en specifik dag. Parameter: day (dag på svenska, t.ex. 'måndag')")
    public String getRecipeForDay(String day) {
        return String.format("""
                👨‍🍳 Recept för %s:

                🍝 Köttfärssås med spagetti

                Ingredienser (4 portioner):
                - 500g köttfärs
                - 1 gul lök, hackad
                - 2 vitlöksklyftor, hackade
                - 400g krossade tomater
                - 2 msk tomatpuré
                - 1 tsk basilika
                - Salt och peppar
                - 400g spagetti

                Tillagning:
                1. Bryn köttfärsen i en stekpanna
                2. Tillsätt lök och vitlök, stek 2-3 min
                3. Häll i krossade tomater och tomatpuré
                4. Krydda med basilika, salt och peppar
                5. Låt sjuda 15-20 min
                6. Koka spagetti enligt förpackning
                7. Servera med riven ost

                ⏱️ Tillagningstid: ~30 min
                💰 Kostnad: ~80 kr
                """, day);
    }
}
