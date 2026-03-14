package dev.pekelund.stock.agent.config;

import io.a2a.server.agentexecution.AgentExecutor;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the A2A (Agent-to-Agent) protocol endpoint.
 * Exposes the Stock Winner agent via the A2A protocol for agent-to-agent communication.
 *
 * <p>Endpoints (relative to context-path /a2a):
 * <ul>
 *   <li>GET  /card     - Agent card discovery (agent metadata and capabilities)</li>
 *   <li>POST /         - Send message to the agent (JSON-RPC 2.0)</li>
 *   <li>GET  /tasks/{id} - Get task status</li>
 * </ul>
 */
@Configuration
public class A2AConfig {

    @Bean
    public AgentCard agentCard(@Value("${server.port:8082}") int port) {
        return new AgentCard.Builder()
                .name("Stock Winner Agent")
                .description("""
                        AI agent för att hitta börsens vinnare med Arne Kavastu Talvings strategi.
                        Analyserar svenska aktier på Stockholmsbörsen baserat på momentum,
                        relativ styrka mot OMXSPI och trendanalys.
                        """)
                .url("http://localhost:" + port + "/a2a/")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("stock_analysis")
                                .name("Aktieanalys")
                                .description("""
                                        Analyserar en specifik aktie med Kavastus momentumstrategi. \
                                        Beräknar 3/6/12-månaders avkastning, relativ styrka mot OMXSPI \
                                        och ger en köp/sälj/bevaka-rekommendation.""")
                                .tags(List.of("aktier", "momentum", "kavastu", "analys"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("portfolio_screening")
                                .name("Portföljscreening")
                                .description("""
                                        Screnar svenska aktier och rekommenderar en portfölj baserad på \
                                        Kavastus principer. Identifierar de aktier med starkast momentum \
                                        och relativ styrka mot OMXSPI.""")
                                .tags(List.of("portfölj", "screening", "kavastu", "vinnare"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("market_analysis")
                                .name("Marknadsanalys")
                                .description("""
                                        Analyserar det övergripande marknadsläget på Stockholmsbörsen. \
                                        Bedömer om marknaden befinner sig i bull eller bear-fas och ger \
                                        råd om lämplig exponering enligt Kavastus marknadsfilter.""")
                                .tags(List.of("marknad", "OMXSPI", "bull", "bear", "trend"))
                                .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient chatClient) {
        return new DefaultAgentExecutor(chatClient, (chat, ctx) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(ctx.getMessage());
            return chat.prompt()
                    .user(userMessage)
                    .call()
                    .content();
        });
    }
}
