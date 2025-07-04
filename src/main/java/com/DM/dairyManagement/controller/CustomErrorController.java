package com.DM.dairyManagement.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Handle all error paths and redirect to login
     * This ensures that any invalid URL will redirect to the login page
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        // Log the error if needed
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
        
        if (exception != null) {
            System.out.println("Error with status code " + statusCode + ": " + exception.getMessage());
        } else {
            System.out.println("Error with status code " + statusCode);
        }
        
        // Redirect to login page for all errors
        return "redirect:/login";
    }
}