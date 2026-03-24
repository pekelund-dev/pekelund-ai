package dev.pekelund.summoner.aspect;

import dev.pekelund.summoner.annotation.CooldownProtected;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP Aspect that enforces cooldown governance on familiar invocations.
 *
 * If the Summoner tries to call the same familiar within the cooldown window
 * (default: 60 seconds), this aspect blocks execution before the familiar
 * is even contacted — without modifying any agent logic.
 *
 * This is the "Plugin" governance layer described in the architecture.
 * Accepts a {@link Clock} for testability.
 */
@Aspect
@Component
public class CooldownAspect {

    private static final Logger log = LoggerFactory.getLogger(CooldownAspect.class);

    private final Clock clock;
    private final ConcurrentHashMap<String, Instant> lastInvocationTimes = new ConcurrentHashMap<>();

    public CooldownAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(cooldownProtected)")
    public Object enforceCooldown(ProceedingJoinPoint joinPoint, CooldownProtected cooldownProtected) throws Throwable {
        String familiarName = cooldownProtected.familiarName();
        long cooldownSeconds = cooldownProtected.cooldownSeconds();

        Instant lastCall = lastInvocationTimes.get(familiarName);
        Instant now = clock.instant();

        if (lastCall != null) {
            long secondsSinceLastCall = now.getEpochSecond() - lastCall.getEpochSecond();
            if (secondsSinceLastCall < cooldownSeconds) {
                long remaining = cooldownSeconds - secondsSinceLastCall;
                log.warn("⛔ COOLDOWN BLOCKED: Familiar '{}' was called {}s ago. Must wait {}s more.",
                         familiarName, secondsSinceLastCall, remaining);
                return "⛔ Familiar '" + familiarName + "' is on cooldown. Please wait " + remaining +
                       " more second(s). (Last called " + secondsSinceLastCall + "s ago)";
            }
        }

        log.info("✅ Cooldown check passed for familiar '{}'. Proceeding with invocation.", familiarName);
        lastInvocationTimes.put(familiarName, now);

        return joinPoint.proceed();
    }

    /**
     * Returns the last invocation time for a familiar (for testing / monitoring).
     */
    public Instant getLastInvocationTime(String familiarName) {
        return lastInvocationTimes.get(familiarName);
    }

    /**
     * Resets the cooldown for a familiar (for testing purposes).
     */
    public void resetCooldown(String familiarName) {
        lastInvocationTimes.remove(familiarName);
    }
}
