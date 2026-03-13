package dev.pekelund.ai.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link IncidentOrchestrator#parseField} helper method.
 *
 * <p>These tests verify the orchestrator can correctly extract structured fields
 * from agent response text, which is critical for populating incident records.
 */
class IncidentOrchestratorParseFieldTest {

    @Test
    void parseField_returnsCorrectValue_forSimpleField() {
        String text = """
                SEVERITY: HIGH
                CATEGORY: DATABASE
                AFFECTED_SERVICES: payment-service,api-gateway
                """;

        assertThat(IncidentOrchestrator.parseField(text, "SEVERITY")).isEqualTo("HIGH");
        assertThat(IncidentOrchestrator.parseField(text, "CATEGORY")).isEqualTo("DATABASE");
    }

    @Test
    void parseField_returnsAffectedServices_withCommaSeparated() {
        String text = "AFFECTED_SERVICES: payment-service,api-gateway,fraud-service\nTEAM: Payments Team\n";
        assertThat(IncidentOrchestrator.parseField(text, "AFFECTED_SERVICES"))
                .isEqualTo("payment-service,api-gateway,fraud-service");
    }

    @Test
    void parseField_returnsUnknown_whenFieldMissing() {
        String text = "SEVERITY: HIGH\nCATEGORY: DATABASE\n";
        assertThat(IncidentOrchestrator.parseField(text, "MISSING_FIELD")).isEqualTo("UNKNOWN");
    }

    @Test
    void parseField_handlesNullInput() {
        assertThat(IncidentOrchestrator.parseField(null, "SEVERITY")).isEqualTo("UNKNOWN");
    }

    @Test
    void parseField_extractsMultilineField() {
        String text = """
                ROOT_CAUSE: Database connection pool exhausted
                CONTRIBUTING_FACTORS:
                1. Long-running batch job
                2. Pool size too small
                EVIDENCE:
                - LOGS: Connection timeout errors
                """;

        String rootCause = IncidentOrchestrator.parseField(text, "ROOT_CAUSE");
        assertThat(rootCause).isEqualTo("Database connection pool exhausted");

        // Contributing factors should capture the multi-line value
        String factors = IncidentOrchestrator.parseField(text, "CONTRIBUTING_FACTORS");
        assertThat(factors).contains("Long-running batch job");
        assertThat(factors).contains("Pool size too small");
    }

    @Test
    void parseField_isCaseSensitiveForFieldName() {
        String text = "SEVERITY: HIGH\n";
        // Field names are uppercase in the agent format
        assertThat(IncidentOrchestrator.parseField(text, "severity")).isEqualTo("UNKNOWN");
        assertThat(IncidentOrchestrator.parseField(text, "SEVERITY")).isEqualTo("HIGH");
    }
}
