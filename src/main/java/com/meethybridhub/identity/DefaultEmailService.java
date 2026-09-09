package com.meethybridhub.identity;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;


@Service
public class DefaultEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailService.class);

    private final JavaMailSenderImpl mailSender;
    private final String from;
    private final String baseUrl;

    public DefaultEmailService(
            @Value("${mail.host:}") String host,
            @Value("${mail.port:587}") int port,
            @Value("${mail.username:}") String username,
            @Value("${mail.password:}") String password,
            @Value("${mail.from:noreply@meethybridhub.com}") String from,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {

        this.from = from;
        this.baseUrl = baseUrl;

        if (host != null && !host.isBlank() && username != null && !username.isBlank()) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            this.mailSender = sender;
            log.info("SMTP email enabled via {}", host);
        } else {
            this.mailSender = null;
            log.info("SMTP not configured (set MAIL_USERNAME/MAIL_PASSWORD) — emails will be logged to the console");
        }
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String token) {
        String link = baseUrl + "/api/v1/auth/verify?token=" + token;
        send(to, "Verify your MeethybridHub email",
                "Hi " + fullName + ",\n\n"
                        + "Please verify your email address by clicking the link below:\n"
                        + link + "\n\n"
                        + "This link expires in 24 hours.\n\n"
                        + "— The MeethybridHub Team");
    }

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String token) {


        String link = baseUrl + "/api/v1/auth/reset-password/confirm?token=" + token;
        send(to, "Reset your MeethybridHub password",
                "Hi " + fullName + ",\n\n"
                        + "We received a request to reset your password. Click the link below to choose a new one:\n"
                        + link + "\n\n"
                        + "This link expires in 1 hour. If you didn't request this, you can safely ignore this email.\n\n"
                        + "— The MeethybridHub Team");
    }

    private void send(String to, String subject, String body) {
        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, false);
                mailSender.send(message);
                log.info("Email sent to {} (subject: {})", to, subject);
            } catch (MessagingException e) {
                log.error("Failed to send email to {}: {}", to, e.getMessage());
                throw new IllegalStateException("Failed to send email", e);
            }
        } else {

            log.info("=== EMAIL (dev mode) ===\nTo: {}\nSubject: {}\nBody:\n{}", to, subject, body);
        }
    }
}
