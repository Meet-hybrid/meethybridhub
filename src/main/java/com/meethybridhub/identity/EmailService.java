package com.meethybridhub.identity;


public interface EmailService {

    void sendVerificationEmail(String to, String fullName, String token);

    void sendPasswordResetEmail(String to, String fullName, String token);
}
