package dev.pekelund.ai.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Spring AI ChatClient configuration for the agent application.
 *
 * <h2>Why a Custom ChatClient?</h2>
 * <p>The default {@link ChatClient} auto-configured by Spring Boot is useful for simple
 * interactions, but the agent application needs a {@code ChatClient} that is pre-wired
 * with the MCP tool callbacks so every agent automatically has access to all tools
 * without having to configure them individually.
 *
 * <h2>How MCP Tools Flow Into the ChatClient</h2>
 * <ol>
 *   <li>The {@code spring-ai-starter-mcp-client} starter creates one
 *       {@link org.springframework.ai.mcp.client.McpSyncClient} per connection defined
 *       in {@code application.yml} and wraps them into a single
 *       {@link ToolCallbackProvider} bean.</li>
 *   <li>This configuration injects that provider into the {@link ChatClient.Builder}
 *       via {@code defaultTools()}, making every tool available to the LLM at
 *       inference time.</li>
 *   <li>When the LLM returns a tool-call in its response, the {@code ChatClient}
 *       resolves the callback, invokes the MCP server, and feeds the result back
 *       into the next prompt turn automatically.</li>
 * </ol>
 *
 * <p>Individual agents can <em>override</em> or <em>extend</em> the default tools
 * by calling {@code .tools()} on their own {@code ChatClient.Builder} if specialised
 * behaviour is required.
 *
 * <p>{@code ToolCallbackProvider} is declared {@link Optional} so this bean can be
 * created even when the MCP client is disabled (e.g., in tests).
 */
@Configuration
public class AgentConfig {

    /**
     * Creates a shared {@link ChatClient} pre-loaded with all MCP tool callbacks.
     *
     * @param builder               Spring AI's auto-configured builder (connected to Gemini)
     * @param toolCallbackProvider  optional tool callbacks from MCP client connections;
     *                              absent when {@code spring.ai.mcp.client.enabled=false}
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 Optional<ToolCallbackProvider> toolCallbackProvider) {
        ChatClient.Builder configured = builder;
        if (toolCallbackProvider.isPresent()) {
            configured = configured.defaultTools(toolCallbackProvider.get());
        }
        return configured.build();
    }
}
