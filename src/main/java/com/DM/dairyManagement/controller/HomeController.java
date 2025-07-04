package com.DM.dairyManagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Redirect root URL to login page
     * This handles the case when users access http://localhost:8080/
     */
    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }
}