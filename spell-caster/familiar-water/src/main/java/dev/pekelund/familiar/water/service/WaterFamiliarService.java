package dev.pekelund.familiar.water.service;

import dev.pekelund.familiar.water.tool.CryoShatterTool;
import dev.pekelund.familiar.water.tool.MoonlightCascadeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;

/**
 * Water Familiar Service — Parallel (fan-out/fan-in) execution pattern.
 *
 * Execution flow:
 * 1. Fan-out: Launch Nexus channeler (cryo_shatter) AND Forge channeler (moonlight_cascade) simultaneously
 *    using Java's StructuredTaskScope for structured concurrency
 * 2. Fan-in: Wait for both to complete
 * 3. Power Merge: Combine both results into a unified attack
 *
 * This demonstrates a parallel agent pattern using Java 21+ StructuredTaskScope.
 */
@Service
public class WaterFamiliarService {

    private static final Logger log = LoggerFactory.getLogger(WaterFamiliarService.class);

    private final CryoShatterTool cryoShatterTool;
    private final MoonlightCascadeTool moonlightCascadeTool;

    public WaterFamiliarService(CryoShatterTool cryoShatterTool, MoonlightCascadeTool moonlightCascadeTool) {
        this.cryoShatterTool = cryoShatterTool;
        this.moonlightCascadeTool = moonlightCascadeTool;
    }

    /**
     * Executes the water familiar's parallel fan-out/fan-in pattern.
     *
     * @param target the attack target
     * @return the power-merged result of both channels
     */
    public String execute(String target) {
        log.info("🌊 Water Familiar activating parallel fan-out/fan-in for target: {}", target);

        // Note: StructuredTaskScope is a preview API in Java 25; requires --enable-preview at compile/runtime.
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<String>allSuccessfulOrThrow())) {
            // Fan-out: launch both channelers simultaneously on virtual threads
            var nexusTask = scope.fork(() -> {
                log.debug("Fan-out → Nexus channeler (cryo_shatter) started");
                return cryoShatterTool.cryoShatter(target);
            });

            var forgeTask = scope.fork(() -> {
                log.debug("Fan-out → Forge channeler (moonlight_cascade) started");
                return moonlightCascadeTool.moonlightCascade(target);
            });

            // Fan-in: wait for both tasks to complete (throws if any failed)
            scope.join();

            String nexusResult = nexusTask.get();
            String forgeResult = forgeTask.get();

            log.debug("Nexus result: {}", nexusResult);
            log.debug("Forge result: {}", forgeResult);

            // Power Merge: combine both channel results
            String merged = powerMerger(nexusResult, forgeResult);
            log.info("🌊 Water Familiar completed. Power merged result: {}", merged);
            return merged;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Water Familiar interrupted during parallel execution", e);
        } catch (Exception e) {
            throw new RuntimeException("Water Familiar parallel execution failed", e);
        }
    }

    /**
     * Power Merger — combines the outputs of the Nexus and Forge channels.
     */
    private String powerMerger(String nexusResult, String forgeResult) {
        return """
               💧 POWER MERGER — Dual-Channel Water Strike:
               ┌─ Nexus Channel: %s
               └─ Forge Channel: %s
               ⚡ Combined Water Power unleashed simultaneously via StructuredTaskScope!
               """.formatted(nexusResult, forgeResult);
    }
}
