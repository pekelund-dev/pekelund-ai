package dev.pekelund.agent.a2a;

import dev.pekelund.agent.config.A2AConfig;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the Spring AI A2A beans ({@link AgentCard} and {@link AgentExecutor})
 * are correctly configured by {@link A2AConfig}.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>The AgentCard is well-formed (required fields non-null, correct skill IDs)</li>
 *   <li>The AgentExecutor bean is present in the Spring context</li>
 *   <li>A2A capabilities match what the synchronous implementation supports</li>
 * </ul>
 *
 * <p>A2A HTTP infrastructure (controllers, task store, queue manager) is disabled in the
 * test profile via {@code spring.ai.a2a.server.enabled=false}. The beans under test
 * ({@link AgentCard}, {@link AgentExecutor}) are created by {@link A2AConfig} and
 * do not require external services.
 */
@SpringBootTest
@ActiveProfiles("test")
class A2AConfigTest {

    @Autowired
    private AgentCard agentCard;

    @Autowired
    private AgentExecutor agentExecutor;

    // ── AgentCard ──────────────────────────────────────────────────────────────

    @Test
    void agentCard_nameAndDescriptionArePresent() {
        assertThat(agentCard.name()).isNotBlank();
        assertThat(agentCard.description()).isNotBlank();
    }

    @Test
    void agentCard_urlAndVersionArePresent() {
        assertThat(agentCard.url()).isNotBlank();
        assertThat(agentCard.url()).contains("/a2a/");
        assertThat(agentCard.version()).isNotBlank();
    }

    @Test
    void agentCard_hasTwoSkills() {
        assertThat(agentCard.skills()).hasSize(2);
        assertThat(agentCard.skills())
                .extracting(AgentSkill::id)
                .containsExactlyInAnyOrder("incident_analysis", "incident_triage");
    }

    @Test
    void agentCard_skillsHaveRequiredFields() {
        for (AgentSkill skill : agentCard.skills()) {
            assertThat(skill.id()).isNotBlank();
            assertThat(skill.name()).isNotBlank();
            assertThat(skill.description()).isNotBlank();
            assertThat(skill.tags()).isNotEmpty();
        }
    }

    @Test
    void agentCard_inputOutputModesAreTextOnly() {
        assertThat(agentCard.defaultInputModes()).contains("text");
        assertThat(agentCard.defaultOutputModes()).contains("text");
    }

    @Test
    void agentCard_capabilitiesMatchSynchronousImplementation() {
        // This implementation is synchronous — streaming and push notifications are false
        assertThat(agentCard.capabilities().streaming()).isFalse();
        assertThat(agentCard.capabilities().pushNotifications()).isFalse();
    }

    @Test
    void agentCard_protocolVersionIsSet() {
        assertThat(agentCard.protocolVersion()).isNotBlank();
    }

    // ── AgentExecutor ──────────────────────────────────────────────────────────

    @Test
    void agentExecutor_isPresent() {
        assertThat(agentExecutor).isNotNull();
    }

    @Test
    void agentExecutor_isDefaultAgentExecutorImpl() {
        // Verify the bean is the standard Spring AI A2A executor
        assertThat(agentExecutor)
                .isInstanceOf(org.springaicommunity.a2a.server.executor.DefaultAgentExecutor.class);
    }
}
