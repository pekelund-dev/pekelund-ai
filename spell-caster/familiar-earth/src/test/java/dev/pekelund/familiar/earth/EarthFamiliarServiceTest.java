package dev.pekelund.familiar.earth;

import dev.pekelund.familiar.earth.service.EarthFamiliarService;
import dev.pekelund.familiar.earth.tool.SeismicChargeTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarthFamiliarServiceTest {

    @Mock
    private SeismicChargeTool seismicChargeTool;

    @InjectMocks
    private EarthFamiliarService earthFamiliarService;

    @Test
    void execute_reachesThreshold_releasesAttackEarly() {
        // Return 35 per charge, so threshold (100) is reached after 3 iterations
        when(seismicChargeTool.seismicCharge(anyInt())).thenReturn(35.0);

        String result = earthFamiliarService.execute("dragon");

        assertThat(result).contains("THRESHOLD REACHED");
        assertThat(result).contains("dragon");
        assertThat(result).contains("Iterations: 3");
    }

    @Test
    void execute_maxIterationsReached_partialRelease() {
        // Return 5 per charge — never reaches threshold of 100 within 10 iterations
        when(seismicChargeTool.seismicCharge(anyInt())).thenReturn(5.0);

        String result = earthFamiliarService.execute("golem");

        assertThat(result).contains("MAX ITERATIONS");
        assertThat(result).contains("Iterations: 10");
    }
}
