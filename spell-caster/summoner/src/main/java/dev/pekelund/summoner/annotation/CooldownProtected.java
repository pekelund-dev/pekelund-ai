package dev.pekelund.summoner.annotation;

import java.lang.annotation.*;

/**
 * Marks a familiar invocation method as subject to cooldown governance.
 * The {@link dev.pekelund.summoner.aspect.CooldownAspect} will intercept
 * methods annotated with this and enforce a 60-second cooldown per familiar.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CooldownProtected {

    /**
     * The name of the familiar being invoked.
     * Used as the key for cooldown tracking.
     */
    String familiarName();

    /**
     * Cooldown duration in seconds. Defaults to 60.
     */
    long cooldownSeconds() default 60;
}
