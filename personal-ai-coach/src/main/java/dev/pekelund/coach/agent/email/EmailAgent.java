package dev.pekelund.coach.agent.email;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * E-postagent — verktyg för att hantera e-post.
 *
 * <p>I denna initiala version ger agenten simulerade e-postdata.
 * I produktion integrerar den med Gmail API via OAuth2.
 */
@Component
public class EmailAgent {

    @Tool(description = "Visa de senaste olästa e-postmeddelandena. Parameter: maxCount (max antal att visa, standard 10)")
    public String getUnreadEmails(int maxCount) {
        return String.format("""
                📧 Du har 7 olästa e-postmeddelanden (visar %d):

                ⚠️  KRÄVER ÅTGÄRD:
                1. [Hög] Anna Svensson — "Granskning av rapport Q1" (2 tim sedan)
                   → Behöver din feedback senast fredag
                2. [Hög] IT-avdelningen — "Obligatorisk säkerhetsuppdatering" (4 tim sedan)
                   → Deadline: imorgon

                📥 INFORMATIONELLT:
                3. Erik Johansson — "Teamlunch nästa vecka" (5 tim sedan)
                4. LinkedIn — "3 nya kontaktförfrågningar" (6 tim sedan)
                5. Spotify — "Din veckosammanfattning" (igår)

                Vill du läsa något specifikt meddelande?""", Math.min(maxCount, 5));
    }

    @Tool(description = "Sök i e-post. Parameter: query (sökterm att leta efter i ämne och avsändare)")
    public String searchEmails(String query) {
        return String.format("""
                🔍 Sökresultat för "%s":

                Hittade 3 matchande e-postmeddelanden:
                1. Anna Svensson — "Möte om %s" (förra veckan)
                2. Chef — "Uppdatering angående %s" (2 veckor sedan)
                3. Nyhetsbrev — "Senaste om %s" (förra månaden)
                """, query, query, query, query);
    }

    @Tool(description = "Visa e-postöversikt med statistik och filter.")
    public String getEmailOverview() {
        return """
                📊 E-postöversikt:

                Inkorg:     47 meddelanden (7 olästa)
                ⚠️ Kräver åtgärd: 3 meddelanden
                📌 Markerade:     5 meddelanden
                🗑️ Papperskorg:  12 meddelanden

                Senaste aktivitet:
                - 2 nya sedan senaste inloggning
                - 1 e-post från VIP-kontakter (chef)

                Konton:
                - Privat (Gmail): 30 meddelanden
                - Arbete (Outlook): 17 meddelanden
                """;
    }

    @Tool(description = "Markera e-post som läst eller åtgärdad. Parameter: emailId (ID för e-postmeddelandet)")
    public String markEmailHandled(String emailId) {
        return String.format("✅ E-post #%s markerad som hanterad.", emailId);
    }
}
