package dev.pekelund.stock.agent.controller;

import dev.pekelund.stock.agent.service.StockAnalysisService;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller providing direct HTTP access to the Stock Winner agent.
 * For agent-to-agent communication, use the A2A endpoints at /a2a/.
 */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockAnalysisService stockAnalysisService;

    public StockController(StockAnalysisService stockAnalysisService) {
        this.stockAnalysisService = stockAnalysisService;
    }

    /**
     * Analyze a specific stock using Kavastu strategy.
     * Example: GET /api/stocks/analyze/ABB.ST
     */
    @GetMapping("/analyze/{symbol}")
    public String analyzeStock(@PathVariable String symbol) {
        return stockAnalysisService.analyzeStock(symbol);
    }

    /**
     * Screen Swedish stocks for Kavastu winners.
     * Example: GET /api/stocks/screen
     */
    @GetMapping("/screen")
    public String screenForWinners() {
        return stockAnalysisService.screenForWinners();
    }

    /**
     * Get current Stockholm market trend analysis.
     * Example: GET /api/stocks/market
     */
    @GetMapping("/market")
    public String analyzeMarket() {
        return stockAnalysisService.analyzeMarket();
    }

    /**
     * Ask a free-form question about stocks.
     * Example: POST /api/stocks/ask  with body: {"question": "Vilka aktier ska jag köpa?"}
     */
    @PostMapping("/ask")
    public String ask(@RequestBody AskRequest request) {
        return stockAnalysisService.ask(request.question());
    }

    record AskRequest(String question) {}
}
