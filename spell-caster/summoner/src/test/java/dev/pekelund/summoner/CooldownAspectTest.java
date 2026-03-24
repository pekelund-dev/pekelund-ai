package dev.pekelund.summoner;

import dev.pekelund.summoner.aspect.CooldownAspect;
import dev.pekelund.summoner.annotation.CooldownProtected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {CooldownAspectTest.TestConfig.class, CooldownAspectTest.TestService.class, CooldownAspect.class})
class CooldownAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private CooldownAspect cooldownAspect;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void setUp() {
        cooldownAspect.resetCooldown("test-familiar");
        cooldownAspect.resetCooldown("test-familiar-short");
        mutableClock.setInstant(Instant.EPOCH);
    }

    @Test
    void firstInvocation_allowed() {
        String result = testService.callFamiliar();
        assertThat(result).isEqualTo("success");
    }

    @Test
    void secondInvocationWithinCooldown_blocked() {
        testService.callFamiliar();
        // Advance by only 30 seconds — still within the 60s cooldown
        mutableClock.setInstant(Instant.EPOCH.plusSeconds(30));
        String result = testService.callFamiliar();
        assertThat(result).contains("cooldown");
    }

    @Test
    void afterCooldownExpires_allowed() {
        testService.callFamiliarShortCooldown();
        // Advance by 2 seconds — past the 1-second cooldown
        mutableClock.setInstant(Instant.EPOCH.plusSeconds(2));
        String result = testService.callFamiliarShortCooldown();
        assertThat(result).isEqualTo("success-short");
    }

    /**
     * A mutable clock whose instant can be controlled in tests.
     */
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.EPOCH);

        public void setInstant(Instant newInstant) {
            instant.set(newInstant);
        }

        @Override
        public ZoneId getZone() { return ZoneId.of("UTC"); }

        @Override
        public Clock withZone(ZoneId zone) { return this; }

        @Override
        public Instant instant() { return instant.get(); }
    }

    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        public MutableClock clock() {
            return new MutableClock();
        }
    }

    @Component
    static class TestService {
        @CooldownProtected(familiarName = "test-familiar")
        public String callFamiliar() {
            return "success";
        }

        @CooldownProtected(familiarName = "test-familiar-short", cooldownSeconds = 1)
        public String callFamiliarShortCooldown() {
            return "success-short";
        }
    }
}
