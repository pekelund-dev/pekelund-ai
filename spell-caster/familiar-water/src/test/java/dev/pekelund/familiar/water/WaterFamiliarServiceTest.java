package dev.pekelund.familiar.water;

import dev.pekelund.familiar.water.service.WaterFamiliarService;
import dev.pekelund.familiar.water.tool.CryoShatterTool;
import dev.pekelund.familiar.water.tool.MoonlightCascadeTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterFamiliarServiceTest {

    @Mock
    private CryoShatterTool cryoShatterTool;

    @Mock
    private MoonlightCascadeTool moonlightCascadeTool;

    @InjectMocks
    private WaterFamiliarService waterFamiliarService;

    @Test
    void execute_parallelPattern_runsBothChannelsAndMerges() {
        when(cryoShatterTool.cryoShatter(anyString())).thenReturn("❄️ CRYO_SHATTER: crystallized");
        when(moonlightCascadeTool.moonlightCascade(anyString())).thenReturn("🌊 MOONLIGHT_CASCADE: engulfed");

        String result = waterFamiliarService.execute("dragon");

        assertThat(result).contains("POWER MERGER");
        assertThat(result).contains("CRYO_SHATTER");
        assertThat(result).contains("MOONLIGHT_CASCADE");
        verify(cryoShatterTool).cryoShatter("dragon");
        verify(moonlightCascadeTool).moonlightCascade("dragon");
    }

    @Test
    void execute_bothChannelsRunSimultaneously_structuredConcurrencyUsed() {
        when(cryoShatterTool.cryoShatter(anyString())).thenReturn("Nexus result");
        when(moonlightCascadeTool.moonlightCascade(anyString())).thenReturn("Forge result");

        String result = waterFamiliarService.execute("target");

        assertThat(result).contains("Nexus result");
        assertThat(result).contains("Forge result");
    }
}
