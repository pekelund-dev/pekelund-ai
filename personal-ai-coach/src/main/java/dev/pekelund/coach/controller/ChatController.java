package dev.pekelund.coach.controller;

import dev.pekelund.coach.agent.AgentService;
import dev.pekelund.coach.agent.ModelRouter;
import dev.pekelund.coach.domain.AgentType;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * HTMX-baserad chattcontroller.
 * Hanterar chattsessioner per agent med HTMX-fragment.
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    private static final String CONVERSATION_PREFIX = "conversation_";

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Visa chattsidan för en specifik agent.
     */
    @GetMapping("/{agentType}")
    public String chatPage(@PathVariable AgentType agentType, Model model) {
        model.addAttribute("agentType", agentType);
        model.addAttribute("agents", AgentType.values());
        return "chat";
    }

    /**
     * HTMX-endpoint: tar emot ett chattmeddelande och returnerar ett HTML-fragment
     * med användarens meddelande och AI:s svar.
     */
    @PostMapping("/{agentType}")
    public String chat(@PathVariable AgentType agentType,
                       @RequestParam String message,
                       @AuthenticationPrincipal OAuth2User principal,
                       HttpSession session,
                       Model model) {
        String conversationKey = CONVERSATION_PREFIX + agentType.name();
        String conversationId = (String) session.getAttribute(conversationKey);
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
            session.setAttribute(conversationKey, conversationId);
        }

        AgentService.ChatResult result = agentService.chat(message, conversationId, agentType);

        model.addAttribute("userMessage", message);
        model.addAttribute("aiResponse", result.content());
        model.addAttribute("modelLabel", result.modelLabel());
        model.addAttribute("modelIcon", result.modelIcon());
        model.addAttribute("isComplex", result.complexity() == ModelRouter.TaskComplexity.COMPLEX);
        return "fragments/message-pair :: message-pair";
    }
}
