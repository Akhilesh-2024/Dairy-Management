package com.DM.dairyManagement.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.DM.dairyManagement.model.User;
import com.DM.dairyManagement.repository.UserRepository;
import com.DM.dairyManagement.service.UserService;

import java.util.Optional;
import java.util.Random;

@Controller
public class ForgotPasswordController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private String generatedCode;
    private String userEmail;
    
    // Show forgot password form
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot_password";
    }

    // Send verification code
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        // Trim the email to remove any whitespace
        email = email.trim();
        
        // Log the email being searched
        System.out.println("Searching for user with email: " + email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "No user found with this email.");
            return "forgot_password";
        }

        userEmail = email;
        generatedCode = generateCode();
        
        // Log the generated code (for debugging only, remove in production)
        System.out.println("Generated verification code: " + generatedCode);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Code");
        message.setText("Your verification code is: " + generatedCode);
        message.setFrom(System.getProperty("MAIL_USERNAME", "your-email@example.com")); // Set the from address to the configured sender email

        try {
            System.out.println("Attempting to send email to: " + email);
            mailSender.send(message);
            System.out.println("Email sent successfully");
            return "redirect:/verify-code";
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace(); // Logs actual error in console
            model.addAttribute("error", "Failed to send email. Try again. Error: " + e.getMessage());
            return "forgot_password";
        }
    }

    // Show verify code form
    @GetMapping("/verify-code")
    public String showVerifyCodeForm() {
        return "verify_code";
    }

    // Process code verification
    @PostMapping("/verify-code")
    public String verifyCode(@RequestParam("code") String code, Model model) {
        if (generatedCode != null && generatedCode.equals(code)) {
            return "redirect:/reset-password";
        } else {
            model.addAttribute("error", "Invalid verification code.");
            return "verify_code";
        }
    }

    // Show reset password form
    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "reset_password_form";
    }

    // Reset password
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "reset_password_form";
        }

        // Use the UserService to reset the password
        User user = userService.getUserByEmail(userEmail);
        
        if (user != null) {
            // Use the service method to properly encode and save the password
            userService.resetPassword(userEmail, password);
            
            // Log the password reset for debugging
            System.out.println("Password reset successful for user: " + userEmail);
            
            model.addAttribute("message", "Password reset successful. You can now login.");
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Something went wrong. User not found.");
            return "reset_password_form";
        }
    }

    // Utility: generate 6-digit code
    private String generateCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}
