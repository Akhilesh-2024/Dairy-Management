package com.DM.dairyManagement.configure;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Get values from environment variables or use defaults
        String host = System.getProperty("MAIL_HOST", "smtp.gmail.com");
        int port = Integer.parseInt(System.getProperty("MAIL_PORT", "587"));
        String username = System.getProperty("MAIL_USERNAME", "");
        String password = System.getProperty("MAIL_PASSWORD", "");
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        
        // Enable debug for troubleshooting
        props.put("mail.debug", "true");

        return mailSender;
    }
}
