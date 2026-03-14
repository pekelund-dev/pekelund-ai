package dev.pekelund.stock.agent.web;

/**
 * Represents a holding in the user's portfolio.
 *
 * @param symbol     Ticker symbol, e.g. ABB.ST
 * @param name       Company name, e.g. ABB Ltd
 * @param shares     Number of shares held
 * @param buyPrice   Purchase price per share in SEK
 */
public record PortfolioEntry(String symbol, String name, double shares, double buyPrice) {

    /** Returns a display-friendly name, falling back to the symbol if name is blank. */
    public String displayName() {
        return (name != null && !name.isBlank()) ? name : symbol;
    }
}
