package dev.pekelund.coach.agent.finance;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Ekonomiagent — verktyg för ekonomisk översikt.
 *
 * <p>I denna initiala version ger agenten simulerade ekonomidata.
 * I produktion integrerar den med aktie-API:er och bankkopplingar.
 */
@Component
public class FinanceAgent {

    @Tool(description = "Visa aktieportföljens översikt med aktuella värden och förändring.")
    public String getStockPortfolio() {
        return """
                📈 Aktieportfölj:

                Aktie            Antal   Kurs       Värde        Förändring
                ─────────────────────────────────────────────────────────────
                Volvo B          100     282,40 kr  28 240 kr    +2,3%
                H&M B           50      165,80 kr   8 290 kr    -1,1%
                Investor B       30      298,60 kr   8 958 kr    +0,8%
                Atlas Copco A    40      196,20 kr   7 848 kr    +1,5%
                Evolution        20      1 024,00 kr 20 480 kr   -0,4%
                ─────────────────────────────────────────────────────────────
                Totalt:                              73 816 kr   +1,2%

                Bäst idag: Volvo B (+2,3%)
                Sämst idag: H&M B (-1,1%)
                """;
    }

    @Tool(description = "Visa sparandeöversikt med olika sparkonton och fonder.")
    public String getSavingsOverview() {
        return """
                💰 Sparandeöversikt:

                Konto/Fond                    Saldo         Avkastning (YTD)
                ────────────────────────────────────────────────────────────
                Sparkonto (Avanza)            45 230 kr     +1,8%
                ISK — Globalfond              128 500 kr    +8,4%
                ISK — Teknikfond              67 800 kr     +12,1%
                Tjänstepension                342 000 kr    +6,2%
                Buffertkonto                  25 000 kr     +0,5%
                ────────────────────────────────────────────────────────────
                Totalt sparande:              608 530 kr    +6,8%
                """;
    }

    @Tool(description = "Visa utgiftsöversikt för aktuell månad med kategorifördelning.")
    public String getMonthlyExpenses() {
        return """
                💳 Utgifter denna månad:

                Kategori              Belopp        Budget      Status
                ──────────────────────────────────────────────────────
                🏠 Boende             8 500 kr      8 500 kr    ✅ I budget
                🛒 Mat & Dryck        4 200 kr      5 000 kr    ✅ Under budget
                🚗 Transport          1 800 kr      2 000 kr    ✅ Under budget
                🎭 Nöje & Fritid      2 100 kr      2 000 kr    ⚠️ Över budget
                👕 Kläder             1 500 kr      1 000 kr    ❌ Över budget
                📱 Abonnemang         890 kr        900 kr      ✅ I budget
                ──────────────────────────────────────────────────────
                Totalt:               18 990 kr     19 400 kr

                ℹ️ Du ligger 410 kr under total budget.
                ⚠️ Kläder har överskridit budget med 500 kr.
                """;
    }

    @Tool(description = "Visa ekonomisk sammanfattning med nyckeltal.")
    public String getFinancialSummary() {
        return """
                📊 Ekonomisk sammanfattning:

                Inkomst (efter skatt):    32 000 kr/mån
                Totala utgifter:          18 990 kr/mån
                Sparkvot:                 40,7%
                Totalt sparande:          608 530 kr
                Aktieportfölj:            73 816 kr

                🎯 Mål: 50% sparkvot → Kvar: 2 810 kr/mån att spara till
                """;
    }
}
