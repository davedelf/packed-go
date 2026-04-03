package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    /**
     * Read HTML template from classpath resources
     */
    private String readTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read email template {}: {}", templateName, e.getMessage());
            throw new RuntimeException("Failed to read email template", e);
        }
    }

    @Override
    public void sendVerificationEmail(String email, String username, String verificationToken) {
        try {
            log.info("Sending verification email via Mailtrap - email: {}, token: {}", email, verificationToken);
                
            String verificationUrl = frontendBaseUrl + "/verify-email?token=" + verificationToken;
            log.info("Generated verification URL: {}", verificationUrl);
            
            String htmlContent = createVerificationEmailTemplate(username, verificationUrl, verificationToken);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Verificación de Email - PackedGo");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("✅ Verification email sent successfully to {} via Mailtrap", email);

        } catch (MessagingException e) {
            log.error("❌ Failed to send verification email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending verification email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String username, String resetToken) {
        try {
            log.info("Sending password reset email via Mailtrap - email: {}", email);
            
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;
            String htmlContent = createPasswordResetEmailTemplate(username, resetUrl, resetToken);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Recuperación de Contraseña - PackedGo");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("✅ Password reset email sent successfully to {} via Mailtrap", email);

        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending password reset email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String createVerificationEmailTemplate(String username, String verificationUrl, String verificationToken) {
        log.info("Creating email template with username: {}, url: {}", username, verificationUrl);
        
        try {
            String template = readTemplate("verification-email.html");
            
            return template
                .replace("{{USERNAME}}", username)
                .replace("{{VERIFICATION_URL}}", verificationUrl);
                
        } catch (Exception e) {
            log.error("Failed to create verification email template: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create verification email template", e);
        }
    }

    private String createPasswordResetEmailTemplate(String username, String resetUrl, String token) {
        try {
            String template = readTemplate("password-reset-email.html");
            
            return template
                .replace("{{USERNAME}}", username)
                .replace("{{RESET_URL}}", resetUrl);
                
        } catch (Exception e) {
            log.error("Failed to create password reset email template: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create password reset email template", e);
        }
    }
}