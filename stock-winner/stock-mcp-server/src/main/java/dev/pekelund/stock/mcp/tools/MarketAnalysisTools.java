package dev.pekelund.stock.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tools implementing Arne Kavastu Talving's investment strategies.
 *
 * <p>Kavastu's key principles implemented here:
 * <ul>
 *   <li>Momentum scoring: weighted 3/6/12-month return analysis</li>
 *   <li>Relative strength vs OMXSPI index</li>
 *   <li>Trend analysis using moving averages</li>
 *   <li>Overall market filter (bull/bear)</li>
 * </ul>
 */
@Component
public class MarketAnalysisTools {

    private static final String YAHOO_CHART_PATH = "/v8/finance/chart/{symbol}";
    private static final String OMXSPI_SYMBOL = "%5EOMXSPI";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MarketAnalysisTools(RestClient yahooFinanceRestClient, ObjectMapper objectMapper) {
        this.restClient = yahooFinanceRestClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Calculate the Kavastu momentum score for a stock.
            Based on Arne Kavastu Talving's strategy: weighted average of 3-month (weight 3),
            6-month (weight 2) and 12-month (weight 1) returns.
            Higher score = stronger momentum = more attractive according to Kavastu strategy.
            Score > 15% is considered strong momentum.
            Score < 0% means the stock is losing momentum.
            """)
    public String calculateMomentumScore(
            @ToolParam(description = "Stock ticker symbol (e.g. ABB.ST, ERIC-B.ST)") String symbol) {
        try {
            double ret3mo = getPeriodReturn(symbol, "3mo");
            double ret6mo = getPeriodReturn(symbol, "6mo");
            double ret12mo = getPeriodReturn(symbol, "1y");

            if (Double.isNaN(ret3mo) || Double.isNaN(ret6mo) || Double.isNaN(ret12mo)) {
                return "Otillräcklig data för momentumberäkning av: " + symbol;
            }

            // Kavastu-inspired weighted momentum score
            // Recent momentum (3mo) gets highest weight, longer term gets lower weight
            double momentumScore = (3 * ret3mo + 2 * ret6mo + ret12mo) / 6.0;

            String rating = getMomentumRating(momentumScore);

            return """
                    Kavastu Momentumanalys för %s:

                    3 månaders avkastning: %+.2f%%
                    6 månaders avkastning: %+.2f%%
                    12 månaders avkastning: %+.2f%%

                    Viktad momentumpoäng: %+.2f%%
                    Betyg: %s

                    Beräkning: (3 × %.2f%% + 2 × %.2f%% + 1 × %.2f%%) / 6
                    """.formatted(
                    symbol,
                    ret3mo, ret6mo, ret12mo,
                    momentumScore, rating,
                    ret3mo, ret6mo, ret12mo);
        } catch (Exception e) {
            return "Fel vid momentumberäkning för %s: %s".formatted(symbol, e.getMessage());
        }
    }

    @Tool(description = """
            Compare a stock's performance relative to the OMXSPI index.
            Kavastu's strategy: invest in stocks that outperform the market index.
            Returns relative strength score and recommendation.
            Positive relative strength = stock is outperforming the market (GOOD).
            Negative relative strength = stock is underperforming the market (AVOID).
            """)
    public String compareToIndex(
            @ToolParam(description = "Stock ticker symbol to compare (e.g. ABB.ST)") String symbol) {
        try {
            double stock3mo = getPeriodReturn(symbol, "3mo");
            double stock6mo = getPeriodReturn(symbol, "6mo");
            double stock12mo = getPeriodReturn(symbol, "1y");

            double index3mo = getPeriodReturn(OMXSPI_SYMBOL, "3mo");
            double index6mo = getPeriodReturn(OMXSPI_SYMBOL, "6mo");
            double index12mo = getPeriodReturn(OMXSPI_SYMBOL, "1y");

            if (Double.isNaN(stock3mo) || Double.isNaN(index3mo)) {
                return "Otillräcklig data för jämförelse av: " + symbol;
            }

            double rel3mo = stock3mo - index3mo;
            double rel6mo = stock6mo - index6mo;
            double rel12mo = stock12mo - index12mo;

            double weightedRelStrength = (3 * rel3mo + 2 * rel6mo + rel12mo) / 6.0;
            String recommendation = getRelativeStrengthRecommendation(weightedRelStrength);

            return """
                    Relativ styrka vs OMXSPI: %s

                    Period     | Aktie    | OMXSPI   | Relativ
                    3 månader  | %+6.2f%% | %+6.2f%% | %+6.2f%%
                    6 månader  | %+6.2f%% | %+6.2f%% | %+6.2f%%
                    12 månader | %+6.2f%% | %+6.2f%% | %+6.2f%%

                    Viktad relativ styrka: %+.2f%%
                    Kavastu-rekommendation: %s
                    """.formatted(
                    symbol,
                    stock3mo, index3mo, rel3mo,
                    stock6mo, index6mo, rel6mo,
                    stock12mo, index12mo, rel12mo,
                    weightedRelStrength,
                    recommendation);
        } catch (Exception e) {
            return "Fel vid indexjämförelse för %s: %s".formatted(symbol, e.getMessage());
        }
    }

    @Tool(description = """
            Analyze the trend direction of a stock using moving averages.
            Evaluates short-term (50-day) and long-term (200-day) moving averages.
            Kavastu strategy: only invest in stocks that are in a clear uptrend.
            Returns trend status and Kavastu suitability.
            """)
    public String analyzeTrend(
            @ToolParam(description = "Stock ticker symbol (e.g. ABB.ST)") String symbol) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(YAHOO_CHART_PATH)
                            .queryParam("interval", "1d")
                            .queryParam("range", "1y")
                            .build(symbol))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("chart").path("result");
            if (results.isEmpty()) {
                return "Ingen data tillgänglig för: " + symbol;
            }

            JsonNode closePrices = results.get(0).path("indicators").path("quote").get(0).path("close");
            List<Double> prices = extractPrices(closePrices);

            if (prices.size() < 50) {
                return "Otillräcklig data för trendanalys (behöver minst 50 datapunkter) för: " + symbol;
            }

            double currentPrice = prices.getLast();
            double ma50 = calculateMovingAverage(prices, 50);
            double ma200 = prices.size() >= 200 ? calculateMovingAverage(prices, 200) : Double.NaN;

            String trendStatus = evaluateTrend(currentPrice, ma50, ma200);
            String kavastuSuitability = getTrendSuitability(currentPrice, ma50, ma200);

            StringBuilder result = new StringBuilder();
            result.append("Trendanalys för %s:%n".formatted(symbol));
            result.append("%n");
            result.append("Aktuellt pris: %.2f%n".formatted(currentPrice));
            result.append("MA50 (50-dagars): %.2f%n".formatted(ma50));
            if (!Double.isNaN(ma200)) {
                result.append("MA200 (200-dagars): %.2f%n".formatted(ma200));
            }
            result.append("%nTrendstatus: %s%n".formatted(trendStatus));
            result.append("Kavastu-lämplighet: %s%n".formatted(kavastuSuitability));

            // Add Golden/Death Cross info if MA200 is available
            if (!Double.isNaN(ma200)) {
                if (ma50 > ma200) {
                    result.append("Signal: Gyllene kors (MA50 > MA200) - Positiv långsiktig trend%n");
                } else {
                    result.append("Signal: Döds-kors (MA50 < MA200) - Negativ långsiktig trend%n");
                }
            }

            return result.toString();
        } catch (Exception e) {
            return "Fel vid trendanalys för %s: %s".formatted(symbol, e.getMessage());
        }
    }

    @Tool(description = """
            Analyze the overall Stockholm stock market trend (OMXSPI).
            Kavastu's market filter: only invest heavily when the overall market is in a bull trend.
            Reduce exposure during bear markets.
            Returns market status (Bull/Bear/Sideways) and investment recommendation.
            """)
    public String getMarketTrend() {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(YAHOO_CHART_PATH)
                            .queryParam("interval", "1wk")
                            .queryParam("range", "2y")
                            .build(OMXSPI_SYMBOL))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("chart").path("result");
            if (results.isEmpty()) {
                return "Kan inte hämta OMXSPI marknadsdata";
            }

            JsonNode closePrices = results.get(0).path("indicators").path("quote").get(0).path("close");
            List<Double> prices = extractPrices(closePrices);

            if (prices.size() < 26) {
                return "Otillräcklig marknadsdata";
            }

            double currentPrice = prices.getLast();
            double ma26w = calculateMovingAverage(prices, Math.min(26, prices.size()));
            double ma52w = prices.size() >= 52 ? calculateMovingAverage(prices, 52) : Double.NaN;

            double ret13w = getPeriodReturn(OMXSPI_SYMBOL, "3mo");
            double ret26w = getPeriodReturn(OMXSPI_SYMBOL, "6mo");
            double ret52w = getPeriodReturn(OMXSPI_SYMBOL, "1y");

            String marketStatus = evaluateMarketStatus(currentPrice, ma26w, ma52w, ret13w, ret52w);
            String investmentAdvice = getMarketInvestmentAdvice(marketStatus);

            return """
                    Marknadsanalys - Stockholmsbörsen (OMXSPI):

                    Aktuell nivå: %.2f
                    26-veckors MA: %.2f%s
                    13 veckors avkastning: %+.2f%%
                    26 veckors avkastning: %+.2f%%
                    52 veckors avkastning: %+.2f%%

                    Marknadsläge: %s
                    Kavastu Råd: %s
                    """.formatted(
                    currentPrice,
                    ma26w,
                    Double.isNaN(ma52w) ? "" : " | 52-veckors MA: %.2f".formatted(ma52w),
                    ret13w, ret26w, ret52w,
                    marketStatus,
                    investmentAdvice);
        } catch (Exception e) {
            return "Fel vid marknadsanalys: " + e.getMessage();
        }
    }

    private double getPeriodReturn(String symbol, String period) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(YAHOO_CHART_PATH)
                            .queryParam("interval", "1wk")
                            .queryParam("range", period)
                            .build(symbol))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode closePrices = root.path("chart").path("result").get(0)
                    .path("indicators").path("quote").get(0).path("close");

            List<Double> prices = extractPrices(closePrices);
            if (prices.size() < 2) return Double.NaN;

            return ((prices.getLast() / prices.getFirst()) - 1.0) * 100.0;
        } catch (RestClientException | JacksonException e) {
            return Double.NaN;
        }
    }

    private List<Double> extractPrices(JsonNode closePrices) {
        List<Double> prices = new ArrayList<>();
        for (JsonNode p : closePrices) {
            if (!p.isNull()) {
                prices.add(p.asDouble());
            }
        }
        return prices;
    }

    private double calculateMovingAverage(List<Double> prices, int period) {
        int start = Math.max(0, prices.size() - period);
        return prices.subList(start, prices.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }

    private String getMomentumRating(double score) {
        if (score >= 25) return "⭐⭐⭐⭐⭐ UTMÄRKT - Stark köpkandidat (Kavastu favorit)";
        if (score >= 15) return "⭐⭐⭐⭐ STARKT MOMENTUM - Köpvärd";
        if (score >= 5)  return "⭐⭐⭐ MÅTTLIGT MOMENTUM - Bevaka";
        if (score >= 0)  return "⭐⭐ SVAGT MOMENTUM - Avvakta";
        return "⭐ NEGATIVT MOMENTUM - Undvik/Sälj";
    }

    private String getRelativeStrengthRecommendation(double relStrength) {
        if (relStrength >= 10) return "STARK ÖVERPRESTANDA - Hög prioritet enligt Kavastu";
        if (relStrength >= 5)  return "ÖVERPRESTANDA - Köpvärd kandidat";
        if (relStrength >= 0)  return "PRESTANDA I LINJE MED INDEX - Neutral";
        if (relStrength >= -5) return "SVAG UNDERPRESTANDA - Undvik eller minska";
        return "KRAFTIG UNDERPRESTANDA - Sälj enligt Kavastu strategi";
    }

    private String evaluateTrend(double price, double ma50, double ma200) {
        if (Double.isNaN(ma200)) {
            if (price > ma50) return "UPPTREND (pris > MA50)";
            return "NEDTREND (pris < MA50)";
        }
        if (price > ma50 && price > ma200 && ma50 > ma200) return "STARK UPPTREND (pris > MA50 > MA200)";
        if (price > ma50 && price > ma200) return "UPPTREND (pris över båda MA)";
        if (price > ma200 && price < ma50) return "BLANDAD SIGNAL (pris mellan MA50 och MA200)";
        if (price < ma50 && price < ma200 && ma50 < ma200) return "STARK NEDTREND (pris < MA50 < MA200)";
        return "NEDTREND";
    }

    private String getTrendSuitability(double price, double ma50, double ma200) {
        if (Double.isNaN(ma200)) {
            return price > ma50 ? "LÄMPLIG för Kavastu strategi" : "EJ LÄMPLIG - vänta på upptrend";
        }
        if (price > ma50 && price > ma200) return "LÄMPLIG - Stark Kavastu kandidat";
        if (price > ma200) return "GRÄNSFALL - Bevaka för bekräftelse";
        return "EJ LÄMPLIG - Kavastu undviker nedtrender";
    }

    private String evaluateMarketStatus(double price, double ma26w, double ma52w, double ret13w, double ret52w) {
        boolean aboveMa26 = price > ma26w;
        boolean positiveMomentum = ret13w > 0 && ret52w > 0;
        boolean aboveMa52 = !Double.isNaN(ma52w) && price > ma52w;

        if (aboveMa26 && aboveMa52 && positiveMomentum) return "BULL MARKET - Starkt uppåttryck";
        if (aboveMa26 && positiveMomentum) return "BULL MARKET - Positivt";
        if (!aboveMa26 && !positiveMomentum) return "BEAR MARKET - Undvik ny exponering";
        if (ret13w < -10) return "KORREKTION - Risk för fortsatt nedgång";
        return "SIDLEDES - Avvakta tydligare trend";
    }

    private String getMarketInvestmentAdvice(String marketStatus) {
        return switch (marketStatus) {
            case String s when s.startsWith("BULL") -> 
                "Fullt investerad - Fokus på hög momentumaktier vs OMXSPI";
            case String s when s.startsWith("KORREKTION") -> 
                "Minska exponering - Behåll bara starkaste positionerna";
            case String s when s.startsWith("BEAR") -> 
                "Defensiv strategi - Minimera aktieexponering, avvakta vändning";
            default -> 
                "Selektiv - Investera enbart i aktier med extremt stark relativ styrka";
        };
    }
}
