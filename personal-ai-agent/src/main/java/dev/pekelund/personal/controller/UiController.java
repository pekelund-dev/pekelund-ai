package dev.pekelund.personal.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class UiController {

    private static final String CONVERSATION_ID_KEY = "conversationId";

    private final ChatClient chatClient;

    public UiController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/")
    public String index() {
        return "chat";
    }

    /**
     * HTMX endpoint: receives a chat message and returns an HTML fragment
     * containing the user message and the AI response to be appended to the
     * messages list via {@code hx-swap="beforeend"}.
     */
    @PostMapping("/chat")
    public String chat(@RequestParam String message,
                       HttpSession session,
                       Model model) {
        String conversationId = (String) session.getAttribute(CONVERSATION_ID_KEY);
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
            session.setAttribute(CONVERSATION_ID_KEY, conversationId);
        }

        final String cid = conversationId;
        String response = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", cid))
                .call()
                .content();

        model.addAttribute("userMessage", message);
        model.addAttribute("aiResponse", response);
        return "fragments/message-pair :: message-pair";
    }
}
