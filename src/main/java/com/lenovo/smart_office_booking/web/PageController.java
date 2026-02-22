package com.lenovo.smart_office_booking.web;

import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.repository.AppUserRepository;
import com.lenovo.smart_office_booking.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final AppUserRepository appUserRepository;
    private final ResourceRepository resourceRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (findCurrentUser(authentication) != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Authentication authentication) {
        if (findCurrentUser(authentication) != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        model.addAttribute("resources", resourceRepository.findAll());
        return "dashboard";
    }

    @GetMapping("/resources")
    public String resources(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "resources";
    }

    @GetMapping("/bookings")
    public String bookings(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "bookings";
    }

    @GetMapping("/bookings/create")
    public String createBooking(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "create-booking";
    }

    @GetMapping("/approvals")
    public String approvals(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "approvals";
    }

    @GetMapping("/admin/registrations")
    public String adminRegistrations(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "admin-registrations";
    }

    @GetMapping("/ai")
    public String aiAssistant(Authentication authentication, Model model) {
        if (!populateCurrentUser(authentication, model)) {
            return "redirect:/login?sessionExpired=true";
        }
        return "ai-assistant";
    }

    private boolean populateCurrentUser(Authentication authentication, Model model) {
        AppUser user = findCurrentUser(authentication);
        if (user == null) {
            return false;
        }

        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("roles", user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()));
        return true;
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return appUserRepository.findByUsername(authentication.getName()).orElse(null);
    }
}