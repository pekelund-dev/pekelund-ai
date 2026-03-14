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
 * MCP tools for fetching stock market data from Yahoo Finance.
 * Provides current quotes, historical prices and a curated list of Swedish stocks.
 */
@Component
public class StockDataTools {

    private static final String YAHOO_FINANCE_CHART_PATH = "/v8/finance/chart/{symbol}";
    private static final String YAHOO_FINANCE_QUOTE_PATH = "/v7/finance/quote";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public StockDataTools(RestClient yahooFinanceRestClient, ObjectMapper objectMapper) {
        this.restClient = yahooFinanceRestClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Get the current stock quote for a given ticker symbol.
            Returns price, daily change, volume and 52-week range.
            For Swedish stocks on Nasdaq Stockholm, use .ST suffix (e.g. ABB.ST, ERIC-B.ST).
            For Stockholm market index, use ^OMXSPI or ^OMXS30.
            """)
    public String getStockQuote(
            @ToolParam(description = "Stock ticker symbol (e.g. ABB.ST, ERIC-B.ST, ^OMXSPI)") String symbol) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(YAHOO_FINANCE_QUOTE_PATH)
                            .queryParam("symbols", symbol)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("quoteResponse").path("result");
            if (results.isEmpty()) {
                return "Ingen data tillgänglig för symbol: " + symbol;
            }

            JsonNode quote = results.get(0);
            double price = quote.path("regularMarketPrice").asDouble();
            double changePercent = quote.path("regularMarketChangePercent").asDouble();
            double change = quote.path("regularMarketChange").asDouble();
            long volume = quote.path("regularMarketVolume").asLong();
            double high52w = quote.path("fiftyTwoWeekHigh").asDouble();
            double low52w = quote.path("fiftyTwoWeekLow").asDouble();
            String currency = quote.path("currency").asText("SEK");
            String longName = quote.path("longName").asText(symbol);
            double ma50 = quote.path("fiftyDayAverage").asDouble();
            double ma200 = quote.path("twoHundredDayAverage").asDouble();

            return """
                    Aktie: %s (%s)
                    Pris: %.2f %s (förändring: %+.2f %s / %+.2f%%)
                    Volym: %,d
                    52-veckors high: %.2f %s
                    52-veckors low: %.2f %s
                    MA50: %.2f | MA200: %.2f
                    Trend: %s
                    """.formatted(
                    longName, symbol,
                    price, currency, change, currency, changePercent,
                    volume,
                    high52w, currency,
                    low52w, currency,
                    ma50, ma200,
                    determineTrend(price, ma50, ma200));
        } catch (RestClientException | JacksonException e) {
            return "Fel vid hämtning av data för %s: %s".formatted(symbol, e.getMessage());
        }
    }

    @Tool(description = """
            Get historical price data for a stock over a specified period.
            Returns closing prices and calculated period returns.
            Useful for momentum analysis and trend identification.
            """)
    public String getHistoricalPrices(
            @ToolParam(description = "Stock ticker symbol (e.g. ABB.ST, ^OMXSPI)") String symbol,
            @ToolParam(description = "Time period: 1mo, 3mo, 6mo, 1y, 2y") String period) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(YAHOO_FINANCE_CHART_PATH)
                            .queryParam("interval", "1wk")
                            .queryParam("range", period)
                            .build(symbol))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result");
            if (result.isEmpty()) {
                return "Ingen historisk data tillgänglig för symbol: " + symbol;
            }

            JsonNode meta = result.get(0).path("meta");
            JsonNode closePrices = result.get(0).path("indicators").path("quote").get(0).path("close");
            String currency = meta.path("currency").asText("SEK");

            if (closePrices.isEmpty()) {
                return "Tom prisserie för symbol: " + symbol;
            }

            List<Double> prices = new ArrayList<>();
            for (JsonNode p : closePrices) {
                if (!p.isNull()) {
                    prices.add(p.asDouble());
                }
            }

            if (prices.size() < 2) {
                return "Otillräcklig prisdata för symbol: " + symbol;
            }

            double firstPrice = prices.getFirst();
            double lastPrice = prices.getLast();
            double periodReturn = ((lastPrice / firstPrice) - 1.0) * 100.0;

            return """
                    Historik för %s (%s):
                    Period: %s
                    Startkurs: %.2f %s
                    Slutkurs: %.2f %s
                    Avkastning: %+.2f%%
                    Antal datapunkter: %d
                    """.formatted(
                    symbol, symbol,
                    period,
                    firstPrice, currency,
                    lastPrice, currency,
                    periodReturn,
                    prices.size());
        } catch (RestClientException | JacksonException e) {
            return "Fel vid hämtning av historisk data för %s: %s".formatted(symbol, e.getMessage());
        }
    }

    @Tool(description = """
            Get a curated list of major Swedish stocks to analyze.
            Returns symbols and company names for the most traded stocks on Nasdaq Stockholm.
            These are representative stocks from OMXS30 and mid-cap segments.
            """)
    public String getSwedishStocksList() {
        return """
                Viktiga svenska aktier på Nasdaq Stockholm (Kavastu-kandidater):

                STORBOLAG (Large Cap / OMXS30):
                - ABB.ST        : ABB Ltd (industriföretag, automation)
                - ERIC-B.ST     : Ericsson B (telekommunikation)
                - VOLV-B.ST     : Volvo B (fordon och maskiner)
                - ASSA-B.ST     : Assa Abloy B (lås och säkerhet)
                - ATCO-A.ST     : Atlas Copco A (industrimaskineri)
                - SEB-A.ST      : SEB A (bank)
                - SHB-A.ST      : Handelsbanken A (bank)
                - SWED-A.ST     : Swedbank A (bank)
                - NDA-SE.ST     : Nordea Bank (bank)
                - INVE-B.ST     : Investor B (investmentbolag)
                - SAND.ST       : Sandvik (industri/gruvutrustning)
                - SKF-B.ST      : SKF B (lager och tätningar)
                - ALIV-SDB.ST   : Autoliv SDB (bilsäkerhet)
                - SSAB-A.ST     : SSAB A (stål)
                - TELIA.ST      : Telia Company (telekommunikation)

                MID CAP / MOMENTUMFAVORITER (typiska Kavastu-aktier):
                - MYCR.ST       : Mycronic (semikonductor-utrustning)
                - LAGR-B.ST     : Lagercrantz B (teknikhandel)
                - BETS-B.ST     : Betsson B (spel/betting)
                - SOBI.ST       : SOBI (läkemedel)
                - MEDC.ST       : Medcap (healthcare)
                - PLEJD.ST      : Plejd (smarta hem)
                - BONE.ST       : Bonesupport (medicinsk teknik)
                - ZINZ-B.ST     : Zinzino B (kosttillskott)
                - NIBE-B.ST     : NIBE Industrier B (värmepumpar)
                - CAST.ST       : Castellum (fastigheter)

                MARKNADSINDEX:
                - ^OMXSPI       : OMXSPI (Stockholmsbörsens breda index)
                - ^OMXS30       : OMXS30 (de 30 mest omsatta aktierna)
                """;
    }

    private String determineTrend(double price, double ma50, double ma200) {
        if (ma50 == 0 || ma200 == 0) return "Data saknas";
        if (price > ma50 && price > ma200 && ma50 > ma200) return "Stark upptrend (Bull)";
        if (price > ma50 && price > ma200) return "Upptrend";
        if (price > ma200 && price < ma50) return "Blandad signal";
        if (price < ma50 && price < ma200) return "Nedtrend (Bear)";
        return "Svag nedtrend";
    }
}
