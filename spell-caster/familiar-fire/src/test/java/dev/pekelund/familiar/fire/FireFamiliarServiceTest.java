package dev.pekelund.familiar.fire;

import dev.pekelund.familiar.fire.service.FireFamiliarService;
import dev.pekelund.familiar.fire.tool.AmplifyTool;
import dev.pekelund.familiar.fire.tool.SpellSearchTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FireFamiliarServiceTest {

    @Mock
    private SpellSearchTool spellSearchTool;

    @Mock
    private AmplifyTool amplifyTool;

    @InjectMocks
    private FireFamiliarService fireFamiliarService;

    @Test
    void execute_sequentialPattern_scoutsAndAmplifies() {
        when(spellSearchTool.scoutSpell("fire")).thenReturn("Scout found: Inferno Blast (power=95) - A massive pillar of fire");
        when(amplifyTool.amplify("Scout found: Inferno Blast (power=95) - A massive pillar of fire"))
                .thenReturn("🔥 AMPLIFIED [x1.5]: Scout found: Inferno Blast (power=95)");

        String result = fireFamiliarService.execute("fire");

        assertThat(result).contains("AMPLIFIED");
        verify(spellSearchTool).scoutSpell("fire");
        verify(amplifyTool).amplify("Scout found: Inferno Blast (power=95) - A massive pillar of fire");
    }

    @Test
    void execute_emptyTarget_defaultsToFireElement() {
        when(spellSearchTool.scoutSpell("fire")).thenReturn("Scout found: Inferno Blast (power=95)");
        when(amplifyTool.amplify("Scout found: Inferno Blast (power=95)")).thenReturn("🔥 AMPLIFIED");

        String result = fireFamiliarService.execute("");

        assertThat(result).isNotNull();
        verify(spellSearchTool).scoutSpell("fire");
    }
}
