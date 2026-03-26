package dev.pekelund.coach.controller;

import dev.pekelund.coach.domain.AgentType;
import dev.pekelund.coach.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Huvudcontroller för dashboarden.
 * Visar översikten med alla agentfunktioner.
 */
@Controller
public class DashboardController {

    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        var user = userService.findOrCreateUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getName());
        model.addAttribute("userPicture", user.getPictureUrl());
        model.addAttribute("agents", AgentType.values());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
