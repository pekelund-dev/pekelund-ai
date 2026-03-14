package dev.pekelund.stock.agent.web;

import dev.pekelund.stock.agent.service.StockAnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Web controller serving Thymeleaf pages for the Stock Winner frontend.
 *
 * <p>All pages are in Swedish as per the user interface requirements.
 *
 * <p>Endpoints (relative to context-path /a2a):
 * <ul>
 *   <li>GET  /              - Home/Dashboard</li>
 *   <li>GET  /historisk     - Historical stock analysis page</li>
 *   <li>POST /historisk/analysera - htmx partial: analyze a stock</li>
 *   <li>GET  /portfölj      - Current portfolio simulation page</li>
 *   <li>POST /portfölj/lägg-till   - htmx partial: add stock to portfolio</li>
 *   <li>POST /portfölj/simulera    - htmx partial: run portfolio simulation</li>
 *   <li>GET  /strategi      - Kavastu strategy explanation page</li>
 * </ul>
 */
@Controller
public class WebController {

    /** Kavastu's typical portfolio size range (number of holdings). */
    private static final String PORTFÖLJ_STORLEK = "50–80";

    // Popular Swedish stocks for the quick-select lists
    static final List<Map<String, String>> SWEDISH_STOCKS = List.of(
            Map.of("symbol", "ABB.ST",      "name", "ABB"),
            Map.of("symbol", "ERIC-B.ST",   "name", "Ericsson B"),
            Map.of("symbol", "VOLV-B.ST",   "name", "Volvo B"),
            Map.of("symbol", "ASSA-B.ST",   "name", "Assa Abloy B"),
            Map.of("symbol", "ATCO-A.ST",   "name", "Atlas Copco A"),
            Map.of("symbol", "SEB-A.ST",    "name", "SEB A"),
            Map.of("symbol", "INVE-B.ST",   "name", "Investor B"),
            Map.of("symbol", "SAND.ST",     "name", "Sandvik"),
            Map.of("symbol", "TELIA.ST",    "name", "Telia"),
            Map.of("symbol", "MYCR.ST",     "name", "Mycronic"),
            Map.of("symbol", "LAGR-B.ST",   "name", "Lagercrantz B"),
            Map.of("symbol", "BETS-B.ST",   "name", "Betsson B"),
            Map.of("symbol", "NIBE-B.ST",   "name", "NIBE Industrier B"),
            Map.of("symbol", "PLEJD.ST",    "name", "Plejd"),
            Map.of("symbol", "BONE.ST",     "name", "Bonesupport")
    );

    private final StockAnalysisService stockAnalysisService;

    // In-memory portfolio (session-like, per-JVM instance — no persistence needed for simulation)
    private final List<PortfolioEntry> portfolio = new ArrayList<>();

    public WebController(StockAnalysisService stockAnalysisService) {
        this.stockAnalysisService = stockAnalysisService;
    }

    // -------------------------------------------------------------------------
    // Startsida (Dashboard)
    // -------------------------------------------------------------------------

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("portföljStorlek", portfolio.size());
        return "index";
    }

    // -------------------------------------------------------------------------
    // Historisk analys
    // -------------------------------------------------------------------------

    @GetMapping("/historisk")
    public String historisk(Model model) {
        model.addAttribute("aktier", SWEDISH_STOCKS);
        return "historisk";
    }

    /**
     * htmx partial: fetch Kavastu analysis for a single stock symbol.
     * Returns an HTML fragment (no full-page layout).
     */
    @PostMapping("/historisk/analysera")
    public String analysera(@RequestParam String symbol, Model model) {
        String analys = stockAnalysisService.analyzeStock(symbol);
        model.addAttribute("symbol", symbol);
        model.addAttribute("analys", analys);
        return "fragments/analysresultat";
    }

    /**
     * htmx partial: run full market screening for Kavastu winners.
     */
    @PostMapping("/historisk/screena")
    public String screena(Model model) {
        String resultat = stockAnalysisService.screenForWinners();
        model.addAttribute("resultat", resultat);
        return "fragments/screeningresultat";
    }

    /**
     * htmx partial: fetch current market trend from OMXSPI.
     */
    @PostMapping("/historisk/marknad")
    public String marknad(Model model) {
        String marknadsanalys = stockAnalysisService.analyzeMarket();
        model.addAttribute("marknadsanalys", marknadsanalys);
        return "fragments/marknadsanalys";
    }

    // -------------------------------------------------------------------------
    // Portfölj (Current portfolio simulation)
    // -------------------------------------------------------------------------

    @GetMapping("/portfölj")
    public String portfölj(Model model) {
        model.addAttribute("aktier", SWEDISH_STOCKS);
        model.addAttribute("portfölj", portfolio);
        return "portfölj";
    }

    /**
     * htmx partial: add a new stock to the portfolio.
     */
    @PostMapping("/portfölj/lägg-till")
    public String läggTill(
            @RequestParam String symbol,
            @RequestParam String name,
            @RequestParam double andelar,
            @RequestParam double köpkurs,
            Model model) {
        portfolio.add(new PortfolioEntry(symbol, name, andelar, köpkurs));
        model.addAttribute("portfölj", portfolio);
        return "fragments/portföljlista";
    }

    /**
     * htmx partial: remove a stock from the portfolio by index.
     */
    @DeleteMapping("/portfölj/{index}")
    public String taBort(@PathVariable int index, Model model) {
        if (index >= 0 && index < portfolio.size()) {
            portfolio.remove(index);
        }
        model.addAttribute("portfölj", portfolio);
        return "fragments/portföljlista";
    }

    /**
     * htmx partial: run portfolio simulation — AI analyzes each holding.
     */
    @PostMapping("/portfölj/simulera")
    public String simulera(Model model) {
        if (portfolio.isEmpty()) {
            model.addAttribute("simuleringsresultat", "Inga aktier i portföljen. Lägg till aktier för att simulera.");
            return "fragments/simuleringsresultat";
        }

        StringBuilder fråga = new StringBuilder();
        fråga.append("Analysera min nuvarande portfölj med Kavastu-strategin och ge rekommendationer:\n\n");
        for (PortfolioEntry entry : portfolio) {
            fråga.append("- %s (%s): %.0f andelar, köpt för %.2f SEK/st\n"
                    .formatted(entry.displayName(), entry.symbol(), entry.shares(), entry.buyPrice()));
        }
        fråga.append("\nFör varje innehav: analysera momentum, relativ styrka mot OMXSPI, och ge tydlig rekommendation (BEHÅLL/ÖKAS/MINSKA/SÄLJ).");
        fråga.append("\nGe också en samlad portföljbedömning baserat på Kavastus principer.");

        String resultat = stockAnalysisService.ask(fråga.toString());
        model.addAttribute("simuleringsresultat", resultat);
        return "fragments/simuleringsresultat";
    }

    // -------------------------------------------------------------------------
    // Strategi
    // -------------------------------------------------------------------------

    @GetMapping("/strategi")
    public String strategi(Model model) {
        model.addAttribute("strategier", buildStrategier());
        return "strategi";
    }

    private List<Map<String, Object>> buildStrategier() {
        List<Map<String, Object>> strategier = new ArrayList<>();

        // Strategy 1: Momentum
        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("nummer", "1");
        s1.put("ikon", "📈");
        s1.put("titel", "Momentum & Trendfölj­ning");
        s1.put("beskrivning", "Investera i aktier med stark positiv trend mätt över 3, 6 och 12 månader. "
                + "Kavastu använder en viktad momentumpoäng: (3×3mån + 2×6mån + 1×12mån) / 6. "
                + "Följ alltid den dominerande trenden – aldrig mot den. "
                + "Aktier som tappar fart ska säljas och kapitalet omallokeras.");
        s1.put("punkter", List.of(
                "Viktat genomsnitt av 3, 6 och 12 månaders avkastning",
                "Aktier med momentumpoäng > 15% är starka köpkandidater",
                "Aldrig investera mot trend",
                "Sälj aktier med negativt momentum"
        ));
        strategier.add(s1);

        // Strategy 2: Relative Strength
        Map<String, Object> s2 = new LinkedHashMap<>();
        s2.put("nummer", "2");
        s2.put("ikon", "⚖️");
        s2.put("titel", "Relativ Styrka mot OMXSPI");
        s2.put("beskrivning", "Prioritera aktier som konsekvent överträffar Stockholmsbörsens breda index (OMXSPI). "
                + "'De starkaste hästarna i loppet' – allokera kapital dit det presterar bäst. "
                + "Om en aktie underpresterar index utan tydlig orsak ska den avyttras.");
        s2.put("punkter", List.of(
                "Jämför 3/6/12-månaders avkastning mot OMXSPI",
                "Positiv relativ styrka = aktien slår marknaden",
                "Vikta upp innehav med starkast relativ styrka",
                "Sälj aktier som kraftigt underpresterar index"
        ));
        strategier.add(s2);

        // Strategy 3: Continuous rebalancing
        Map<String, Object> s3 = new LinkedHashMap<>();
        s3.put("nummer", "3");
        s3.put("ikon", "🔄");
        s3.put("titel", "Kontinuerlig Ombalansering");
        s3.put("beskrivning", "Ingen aktie är 'för bra' att sälja om trenden vänder. "
                + "Ombalansera portföljen regelbundet genom att sälja det som tappar fart "
                + "och reinvestera i det som accelererar. Var beredd att skifta snabbt.");
        s3.put("punkter", List.of(
                "Regelbunden genomgång av alla innehav",
                "Sälj förlorare utan sentimentalitet",
                "Reinvestera i aktier med stärkande momentum",
                "Ingen aktie är 'permanent' i portföljen"
        ));
        strategier.add(s3);

        // Strategy 4: Market filter
        Map<String, Object> s4 = new LinkedHashMap<>();
        s4.put("nummer", "4");
        s4.put("ikon", "🌡️");
        s4.put("titel", "Marknadsfilter (Bull/Bear)");
        s4.put("beskrivning", "Kontrollera alltid det övergripande marknadsläget (OMXSPI) INNAN investering. "
                + "Under en bull-marknad: investera fullt ut i hög-momentum aktier. "
                + "Under en bear-marknad: minska aktieexponeringen markant och behåll bara de allra starkaste.");
        s4.put("punkter", List.of(
                "OMXSPI över MA26W och MA52W = Bull-marknad",
                "Fullt investerad under bull, defensiv under bear",
                "Marknads-trend viktigare än enskilda aktier",
                "MA50/MA200-kors (Golden/Death Cross) som signaler"
        ));
        strategier.add(s4);

        // Strategy 5: Risk management
        Map<String, Object> s5 = new LinkedHashMap<>();
        s5.put("nummer", "5");
        s5.put("ikon", "🛡️");
        s5.put("titel", "Riskhantering");
        s5.put("beskrivning", "Undvik spekulation och 'lotteriaktier'. Fokus på välestablerade bolag "
                + "med stabil omsättningstillväxt och stigande vinster. "
                + "Diversifiera men viktad mot de starkaste innehaven (ca 40% av portföljen).");
        s5.put("punkter", List.of(
                "Undvik bolag utan bevisad lönsamhet",
                "Stor diversifiering – typiskt " + PORTFÖLJ_STORLEK + " innehav",
                "Största positionerna ~40% av total portfölj",
                "Sätt stop-loss mentalt vid trendbrott"
        ));
        strategier.add(s5);

        // Strategy 6: Portfolio composition
        Map<String, Object> s6 = new LinkedHashMap<>();
        s6.put("nummer", "6");
        s6.put("ikon", "🎯");
        s6.put("titel", "Portföljsammansättning");
        s6.put("beskrivning", "Kavastu håller en bred portfölj men viktar den mot de starkaste innehaven. "
                + "Typiskt " + PORTFÖLJ_STORLEK + " aktier där de 10-15 starkaste utgör kärnan. "
                + "Koncentrationen ökar i bull-marknader och minskar i bear-marknader.");
        s6.put("punkter", List.of(
                "Bred bas med " + PORTFÖLJ_STORLEK + " innehav",
                "De 10-15 starkaste = portföljkärnan",
                "Viktning baserad på momentumstyrka",
                "Koncentrera mer i tydliga bull-marknader"
        ));
        strategier.add(s6);

        return strategier;
    }
}
