package dev.pekelund.coach.controller;

import dev.pekelund.coach.agent.AgentService;
import dev.pekelund.coach.agent.ModelRouter;
import dev.pekelund.coach.domain.AgentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AgentService agentService() {
            AgentService service = mock(AgentService.class);
            when(service.chat(anyString(), anyString(), any(AgentType.class)))
                    .thenReturn(new AgentService.ChatResult(
                            "Hej! Jag kan hjälpa dig.",
                            "gemini-2.0-flash",
                            ModelRouter.TaskComplexity.SIMPLE,
                            AgentType.GENERAL));
            return service;
        }
    }

    @Test
    @WithMockUser
    void chatEndpointReturnsResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"message": "Hej!", "agentType": "GENERAL"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Hej! Jag kan hjälpa dig."))
                .andExpect(jsonPath("$.modelUsed").value("gemini-2.0-flash"))
                .andExpect(jsonPath("$.agentType").value("GENERAL"));
    }

    @Test
    void chatEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"message": "Hej!"}
                            """))
                .andExpect(status().isUnauthorized());
    }
}
