package dev.pekelund.familiar.fire.tool;

import org.springframework.stereotype.Component;

/**
 * Tool that amplifies a spell's effect.
 * Represents an "Imperative API Tool" that enhances a scouted spell.
 */
@Component
public class AmplifyTool {

    private static final double FIRE_AMPLIFICATION_FACTOR = 1.5;

    /**
     * Amplifies the given spell result with a fire enhancement.
     */
    public String amplify(String spellResult) {
        if (spellResult.startsWith("No ") || spellResult.startsWith("Scout found: No")) {
            return "Cannot amplify: " + spellResult;
        }
        return "🔥 AMPLIFIED [x" + FIRE_AMPLIFICATION_FACTOR + "]: " + spellResult +
               " → Enhanced with Ignis Amplification Protocol. Effective power multiplied by " + FIRE_AMPLIFICATION_FACTOR + "!";
    }
}
