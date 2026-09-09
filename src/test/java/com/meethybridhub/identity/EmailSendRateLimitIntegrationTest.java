package com.meethybridhub.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailSendRateLimitIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void resendVerificationIsLimitedPerEmail() throws Exception {
        registerPending("flood-resend@example.com");


        for (int i = 0; i < 3; i++) {
            resendVerification("flood-resend@example.com", 200);
        }


        resendVerification("flood-resend@example.com", 429);
    }

    @Test
    void resetPasswordIsLimitedPerEmail() throws Exception {
        registerAndActivate("flood-reset@example.com");

        for (int i = 0; i < 3; i++) {
            requestPasswordReset("flood-reset@example.com", 200);
        }

        requestPasswordReset("flood-reset@example.com", 429);
    }

    @Test
    void emailRequestsAreLimitedPerIp() throws Exception {


        for (int i = 0; i < 10; i++) {
            requestPasswordReset("flood-ip-" + i + "@example.com", 200);
        }

        requestPasswordReset("flood-ip-10@example.com", 429);
    }

    @Test
    void emailSendsDoNotCountTowardLoginIpLimit() throws Exception {

        for (int i = 0; i < 5; i++) {
            requestPasswordReset("isolation-" + i + "@example.com", 200);
        }


        registerAndActivate("isolation-login@example.com");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"isolation-login@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private void resendVerification(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().is(expectedStatus));
    }

    private void requestPasswordReset(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().is(expectedStatus));
    }

    private void registerPending(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", "Flood Test User"))))
                .andExpect(status().isCreated());
    }

    private void registerAndActivate(String email) throws Exception {
        registerPending(email);

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
