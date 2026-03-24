package dev.pekelund.summoner;

import dev.pekelund.summoner.aspect.CooldownAspect;
import dev.pekelund.summoner.annotation.CooldownProtected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {CooldownAspectTest.TestConfig.class, CooldownAspectTest.TestService.class, CooldownAspect.class})
class CooldownAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private CooldownAspect cooldownAspect;

    @BeforeEach
    void setUp() {
        cooldownAspect.resetCooldown("test-familiar");
        cooldownAspect.resetCooldown("test-familiar-short");
    }

    @Test
    void firstInvocation_allowed() {
        String result = testService.callFamiliar();
        assertThat(result).isEqualTo("success");
    }

    @Test
    void secondInvocationWithinCooldown_blocked() {
        testService.callFamiliar();
        String result = testService.callFamiliar();
        assertThat(result).contains("cooldown");
    }

    @Test
    void afterCooldownExpires_allowed() throws InterruptedException {
        // Use 1-second cooldown for testing
        testService.callFamiliarShortCooldown();
        Thread.sleep(1100);
        String result = testService.callFamiliarShortCooldown();
        assertThat(result).isEqualTo("success-short");
    }

    @EnableAspectJAutoProxy
    static class TestConfig {}

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
