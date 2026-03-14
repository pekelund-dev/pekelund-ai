package dev.pekelund.stock.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for stock analysis using the configured ChatClient with Kavastu strategy.
 * Used by the REST controller for direct HTTP access to the agent.
 */
@Service
public class StockAnalysisService {

    private final ChatClient chatClient;

    public StockAnalysisService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Analyzes a specific stock using Kavastu momentum strategy.
     *
     * @param symbol the stock ticker symbol (e.g. ABB.ST, ERIC-B.ST)
     * @return analysis result with momentum score, relative strength and recommendation
     */
    public String analyzeStock(String symbol) {
        return chatClient.prompt()
                .user("Analysera aktien %s med Kavastu-strategin. Beräkna momentum score, jämför med OMXSPI och ge en tydlig rekommendation (KÖP/BEVAKA/SÄLJ).".formatted(symbol))
                .call()
                .content();
    }

    /**
     * Screens Swedish stocks for Kavastu winners.
     *
     * @return list of top momentum stocks recommended by the agent
     */
    public String screenForWinners() {
        return chatClient.prompt()
                .user("""
                        Screena de viktigaste svenska aktierna och hitta börsens vinnare just nu.
                        1. Kontrollera marknadsläget (OMXSPI)
                        2. Analysera topp-kandidater från Large Cap och Mid Cap
                        3. Rangordna efter Kavastu momentum score
                        4. Ge en rekommenderad portfölj med de 5-10 starkaste aktierna
                        """)
                .call()
                .content();
    }

    /**
     * Analyzes the current Stockholm stock market trend.
     *
     * @return market analysis with bull/bear status and investment advice
     */
    public String analyzeMarket() {
        return chatClient.prompt()
                .user("Analysera det aktuella marknadsläget på Stockholmsbörsen (OMXSPI). Är vi i bull eller bear? Vad rekommenderar Kavastu-strategin?")
                .call()
                .content();
    }

    /**
     * Answers a free-form question about stocks using the Kavastu framework.
     *
     * @param question the user's question in Swedish or English
     * @return the agent's answer
     */
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
