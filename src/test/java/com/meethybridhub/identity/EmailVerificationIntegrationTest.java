package com.meethybridhub.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the email verification workflow (Hybrid's Card 2):
 *   - Registration persists a one-time verification token and emails it
 *   - GET /api/v1/auth/verify activates the user and consumes the token
 *   - Login works only AFTER verification
 *   - Invalid / expired / replayed tokens are rejected
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailVerificationIntegrationTest {

    private static final String EMAIL = "verify@example.com";
    private static final String PASSWORD = "TestPassword123!";
    private static final String NAME = "Verify User";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationCreatesPendingUserWithVerificationToken() throws Exception {
        registerUser();

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.PENDING);
        assertThat(user.isEmailVerified()).isFalse();

        EmailVerificationToken token = tokenRepository.findByUserId(user.getId()).get(0);
        assertThat(token.getToken()).hasSize(64);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void verifyEmailActivatesUserAndConsumesToken() throws Exception {
        registerUser();

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();

        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User verified = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(verified.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(verified.isEmailVerified()).isTrue();

        assertThat(tokenRepository.findByUserId(user.getId()).get(0).isUsed()).isTrue();
    }

    @Test
    void loginSucceedsAfterVerification() throws Exception {
        registerUser();

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();

        // Before verification login is rejected (user disabled)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());

        // After verification login succeeds
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void unverifiedUserCannotUseAuthenticatedEndpoints() throws Exception {
        // Tokens issued at registration must NOT work until the email is verified.
        String accessToken = registerUser();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        // After verification the same token works.
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify").param("token", "not-a-real-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        // Build a user + already-expired token directly (bypassing registration)
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), NAME);
        user.setStatus(User.UserStatus.PENDING);
        userRepository.save(user);

        EmailVerificationToken expired = new EmailVerificationToken(
                user.getId(), "a".repeat(64), Instant.now().minus(1, ChronoUnit.HOURS));
        tokenRepository.save(expired);

        mockMvc.perform(get("/api/v1/auth/verify").param("token", expired.getToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenCannotBeReplayed() throws Exception {
        registerUser();

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();

        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk());

        // Second use of the same token is rejected
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isBadRequest());
    }

    private String registerUser() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "password", PASSWORD,
                                "fullName", NAME))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
