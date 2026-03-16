package dev.pekelund.personal.controller;

import dev.pekelund.personal.agent.AgentService;
import dev.pekelund.personal.agent.ModelRouter;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class UiController {

    private static final String CONVERSATION_ID_KEY = "conversationId";

    private final AgentService agentService;

    public UiController(AgentService agentService) {
        this.agentService = agentService;
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

        AgentService.ChatResult result = agentService.chat(message, conversationId);

        model.addAttribute("userMessage", message);
        model.addAttribute("aiResponse", result.content());
        model.addAttribute("modelLabel", result.modelLabel());
        model.addAttribute("modelIcon", result.modelIcon());
        model.addAttribute("isComplex", result.complexity() == ModelRouter.TaskComplexity.COMPLEX);
        return "fragments/message-pair :: message-pair";
    }
}

