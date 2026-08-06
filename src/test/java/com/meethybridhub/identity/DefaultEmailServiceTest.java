package com.meethybridhub.identity;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link DefaultEmailService} — no Spring context.
 *
 * The constructor branches on whether SMTP credentials are configured:
 *   - configured  → builds a real JavaMailSenderImpl and actually sends
 *   - unconfigured → logs the email to the console (dev/test mode)
 *
 * For the SMTP path the sender is swapped for a mock via
 * {@link ReflectionTestUtils} so no network is ever touched; the same trick
 * lets us trigger the {@link MessagingException} catch deterministically.
 */
class DefaultEmailServiceTest {

    private static final String FROM = "noreply@meethybridhub.com";
    private static final String BASE_URL = "https://api.example.com";

    /** Blank host + username → SMTP not configured → console logging mode. */
    private DefaultEmailService devModeService() {
        return new DefaultEmailService("", 587, "", "", FROM, BASE_URL);
    }

    /** Configures SMTP (covers the constructor's sender-building branch), then swaps in a mock sender. */
    private DefaultEmailService smtpService(JavaMailSenderImpl sender) {
        DefaultEmailService service =
                new DefaultEmailService("smtp.example.com", 587, "user", "pass", FROM, BASE_URL);
        ReflectionTestUtils.setField(service, "mailSender", sender);
        return service;
    }

    private JavaMailSenderImpl fakeSender() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        return sender;
    }

    @Test
    void devModeEmailsAreLoggedWithoutError() {
        DefaultEmailService service = devModeService();

        assertThatCode(() -> service.sendVerificationEmail("alice@example.com", "Alice", "tok123"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.sendPasswordResetEmail("bob@example.com", "Bob", "tok456"))
                .doesNotThrowAnyException();
    }

    @Test
    void smtpDisabledWhenHostSetButUsernameBlank() {
        // host present but username blank → the SMTP condition short-circuits to
        // false and the service falls back to dev mode (covers the remaining
        // branches of the constructor guard).
        DefaultEmailService service =
                new DefaultEmailService("smtp.example.com", 587, "", "", FROM, BASE_URL);

        assertThatCode(() -> service.sendVerificationEmail("alice@example.com", "Alice", "tok123"))
                .doesNotThrowAnyException();
    }

    @Test
    void verificationEmailIsSentViaSmtpWithCorrectContent() throws Exception {
        JavaMailSenderImpl sender = fakeSender();
        DefaultEmailService service = smtpService(sender);

        service.sendVerificationEmail("alice@example.com", "Alice", "tok123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Verify your MeethybridHub email");
        assertThat(sent.getAllRecipients()).hasSize(1);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("alice@example.com");
        assertThat(sent.getContent().toString())
                .contains(BASE_URL + "/api/v1/auth/verify?token=tok123")
                .contains("Alice")
                .contains("expires in 24 hours");
    }

    @Test
    void passwordResetEmailIsSentViaSmtpWithCorrectLink() throws Exception {
        JavaMailSenderImpl sender = fakeSender();
        DefaultEmailService service = smtpService(sender);

        service.sendPasswordResetEmail("bob@example.com", "Bob", "tok456");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Reset your MeethybridHub password");
        assertThat(sent.getContent().toString())
                .contains(BASE_URL + "/api/v1/auth/reset-password/confirm?token=tok456")
                .contains("expires in 1 hour");
    }

    @Test
    void unparseableRecipientThrowsIllegalStateExceptionWrappingMessagingException() {
        JavaMailSenderImpl sender = fakeSender();
        DefaultEmailService service = smtpService(sender);

        // Mockito can't stub a checked MessagingException on send() (the method only
        // declares MailException), so drive the real trigger instead: an unparseable
        // recipient makes MimeMessageHelper.setTo throw an AddressException (a
        // MessagingException), which send() catches and rethrows.
        assertThatThrownBy(() -> service.sendVerificationEmail("not an email", "Alice", "tok123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to send email")
                .hasCauseInstanceOf(MessagingException.class);
    }
}
