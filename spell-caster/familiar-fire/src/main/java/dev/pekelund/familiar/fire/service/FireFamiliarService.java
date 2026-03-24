package dev.pekelund.familiar.fire.service;

import dev.pekelund.familiar.fire.tool.AmplifyTool;
import dev.pekelund.familiar.fire.tool.SpellSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fire Familiar Service — Sequential execution pattern.
 *
 * Execution flow:
 * 1. Scout: Search the Librarium DB for a fire spell
 * 2. Amplify: Enhance the found spell with fire amplification
 *
 * This demonstrates a sequential (linear dependency) agent pattern.
 */
@Service
public class FireFamiliarService {

    private static final Logger log = LoggerFactory.getLogger(FireFamiliarService.class);

    private final SpellSearchTool spellSearchTool;
    private final AmplifyTool amplifyTool;

    public FireFamiliarService(SpellSearchTool spellSearchTool, AmplifyTool amplifyTool) {
        this.spellSearchTool = spellSearchTool;
        this.amplifyTool = amplifyTool;
    }

    /**
     * Executes the fire familiar's sequential attack pattern.
     *
     * @param target the attack target or element to search for
     * @return the amplified spell result
     */
    public String execute(String target) {
        log.info("🔥 Fire Familiar activating sequential pattern for target: {}", target);

        // Step 1 — Scout: search the Librarium DB
        String element = target.isBlank() ? "fire" : target;
        log.debug("Step 1 — Scout: searching Librarium for '{}' spells", element);
        String scouted = spellSearchTool.scoutSpell(element);
        log.debug("Scout result: {}", scouted);

        // Step 2 — Amplify: enhance the scouted spell
        log.debug("Step 2 — Amplify: enhancing scouted spell");
        String amplified = amplifyTool.amplify(scouted);
        log.info("🔥 Fire Familiar completed. Result: {}", amplified);

        return amplified;
    }
}
