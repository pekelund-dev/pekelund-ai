package dev.pekelund.familiar.water.tool;

import org.springframework.stereotype.Component;

/**
 * Imperative API tool: cryo_shatter
 * Channels the Nexus icy energy to shatter targets with crystalline force.
 */
@Component
public class CryoShatterTool {

    /**
     * Executes the cryo_shatter attack through the Nexus channel.
     *
     * @param target the attack target
     * @return the channeled Nexus cryo-shatter result
     */
    public String cryoShatter(String target) {
        int power = 70 + (int)(Math.random() * 25);
        return "❄️ CRYO_SHATTER [Nexus Channel]: Target '" + target +
               "' crystallized and shattered with " + power + " ice power. " +
               "Nexus energy threads: " + Thread.currentThread().getName();
    }
}
