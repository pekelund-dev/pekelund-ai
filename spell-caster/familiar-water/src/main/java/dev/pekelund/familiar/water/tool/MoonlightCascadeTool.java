package dev.pekelund.familiar.water.tool;

import org.springframework.stereotype.Component;

/**
 * Imperative API tool: moonlight_cascade
 * Channels the Forge lunar energy in a cascading water torrent.
 */
@Component
public class MoonlightCascadeTool {

    /**
     * Executes the moonlight_cascade attack through the Forge channel.
     *
     * @param target the attack target
     * @return the channeled Forge moonlight-cascade result
     */
    public String moonlightCascade(String target) {
        int power = 80 + (int)(Math.random() * 20);
        return "🌊 MOONLIGHT_CASCADE [Forge Channel]: Target '" + target +
               "' engulfed in lunar water cascade with " + power + " moon power. " +
               "Forge energy threads: " + Thread.currentThread().getName();
    }
}
