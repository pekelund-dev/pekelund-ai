package dev.pekelund.familiar.earth.service;

import dev.pekelund.familiar.earth.tool.SeismicChargeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Earth Familiar Service — Iterative (loop until condition) execution pattern.
 *
 * Execution flow:
 * 1. Initialize energy accumulator
 * 2. Loop: call seismic_charge to accumulate energy
 * 3. Repeat until energy threshold is reached OR max iterations exceeded
 * 4. Release the attack
 *
 * This demonstrates an iterative agent pattern with a feedback loop.
 */
@Service
public class EarthFamiliarService {

    private static final Logger log = LoggerFactory.getLogger(EarthFamiliarService.class);
    private static final double ENERGY_THRESHOLD = 100.0;
    private static final int MAX_ITERATIONS = 10;

    private final SeismicChargeTool seismicChargeTool;

    public EarthFamiliarService(SeismicChargeTool seismicChargeTool) {
        this.seismicChargeTool = seismicChargeTool;
    }

    /**
     * Executes the earth familiar's iterative charge-and-release pattern.
     *
     * @param target the attack target
     * @return the seismic attack result with accumulated energy details
     */
    public String execute(String target) {
        log.info("🌍 Earth Familiar activating iterative charge pattern for target: {}", target);

        double totalEnergy = 0.0;
        int iteration = 0;
        StringBuilder chargeLog = new StringBuilder();

        // Iterative loop: accumulate seismic energy until threshold or max iterations
        while (totalEnergy < ENERGY_THRESHOLD && iteration < MAX_ITERATIONS) {
            iteration++;
            double charge = seismicChargeTool.seismicCharge(iteration);
            totalEnergy += charge;
            chargeLog.append(String.format("  Charge #%d: +%.1f energy (total: %.1f)%n", iteration, charge, totalEnergy));
            log.debug("Seismic charge #{}: +{} energy, total: {}", iteration, charge, totalEnergy);
        }

        String outcome = totalEnergy >= ENERGY_THRESHOLD
            ? "✅ THRESHOLD REACHED — Attack released!"
            : "⚠️ MAX ITERATIONS reached — Partial energy release.";

        String result = """
                🌍 SEISMIC_CHARGE Iterative Pattern — Attack on '%s':
                %s
                %s
                Total energy: %.1f | Iterations: %d | Threshold: %.1f
                """.formatted(target, chargeLog.toString().trim(), outcome, totalEnergy, iteration, ENERGY_THRESHOLD);

        log.info("🌍 Earth Familiar completed. Energy: {}, iterations: {}", totalEnergy, iteration);
        return result;
    }
}
