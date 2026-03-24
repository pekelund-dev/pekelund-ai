package dev.pekelund.summoner.model;

/**
 * View-model for a familiar's current status, used by the Web UI sidebar.
 *
 * @param name              machine name (e.g. "fire-familiar")
 * @param displayName       human-readable label with emoji
 * @param pattern           execution pattern label (Sequential / Parallel / Iterative)
 * @param port              HTTP port this familiar runs on
 * @param online            true if the familiar was discovered at startup
 * @param cooldownRemaining seconds remaining on cooldown (0 = ready)
 * @param maxCooldownSeconds configured cooldown window in seconds (used for the progress bar)
 */
public record FamiliarStatus(
        String name,
        String displayName,
        String pattern,
        int port,
        boolean online,
        long cooldownRemaining,
        long maxCooldownSeconds) {

    /** CSS element class matching the familiar's element (fire / water / earth). */
    public String elementClass() {
        if (name().contains("fire")) return "fire";
        if (name().contains("water")) return "water";
        return "earth";
    }

    /**
     * Percentage of the cooldown bar to fill (0–100).
     * Full bar = just entered cooldown; empty bar = ready.
     *
     * @param maxCooldownSeconds the configured cooldown window for this familiar
     */
    public int cooldownPercent(long maxCooldownSeconds) {
        if (cooldownRemaining() <= 0 || maxCooldownSeconds <= 0) return 0;
        return (int) Math.min(100, (cooldownRemaining() * 100.0) / maxCooldownSeconds);
    }

    /** CSS badge class for online/cooldown/offline state. */
    public String statusBadge() {
        if (!online()) return "offline";
        if (cooldownRemaining() > 0) return "cooldown";
        return "online";
    }

    /** Human-readable status label. */
    public String statusLabel() {
        if (!online()) return "Offline";
        if (cooldownRemaining() > 0) return "Cooldown " + cooldownRemaining() + "s";
        return "Ready";
    }
}
