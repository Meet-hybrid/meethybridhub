package com.meethybridhub.identity;

/**
 * Sends transactional emails (verification links, password resets).
 *
 * Kept as an interface so the delivery mechanism can vary without touching
 * the callers: real SMTP in production, console logging in development/tests.
 */
public interface EmailService {

    void sendVerificationEmail(String to, String fullName, String token);

    void sendPasswordResetEmail(String to, String fullName, String token);
}
