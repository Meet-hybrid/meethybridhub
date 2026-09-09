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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    private static final String EMAIL = "reset@example.com";
    private static final String PASSWORD = "TestPassword123!";
    private static final String NEW_PASSWORD = "NewPassword456!";
    private static final String NAME = "Reset User";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void requestResetForExistingUserIssuesOneTimeToken() throws Exception {
        registerAndActivate();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());

        List<PasswordResetToken> tokens = resetTokenRepository.findByUserId(user(EMAIL).getId());
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getToken()).hasSize(64);
        assertThat(tokens.get(0).getExpiresAt()).isAfter(Instant.now());
        assertThat(tokens.get(0).isUsed()).isFalse();
    }

    @Test
    void requestResetForUnknownEmailStillReturnsOk() throws Exception {

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ghost@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmResetUpdatesPasswordAndAllowsLoginWithNewPassword() throws Exception {
        registerAndActivate();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());

        String token = resetTokenRepository.findByUserId(user(EMAIL).getId()).get(0).getToken();

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmResetWithInvalidTokenFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"bogus-token\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmResetWithExpiredTokenFails() throws Exception {
        User user = createUser();
        resetTokenRepository.save(new PasswordResetToken(
                user.getId(), "b".repeat(64), Instant.now().minus(1, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + "b".repeat(64) + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetTokenCannotBeReplayed() throws Exception {
        registerAndActivate();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());

        String token = resetTokenRepository.findByUserId(user(EMAIL).getId()).get(0).getToken();

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());


        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationInvalidatesOldTokenAndIssuesFreshOne() throws Exception {
        registerUser();

        User user = user(EMAIL);
        String oldToken = verificationTokenRepository.findByUserId(user.getId()).get(0).getToken();

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());


        String newToken = verificationTokenRepository.findByUserId(user.getId()).get(0).getToken();
        assertThat(newToken).isNotEqualTo(oldToken);

        mockMvc.perform(get("/api/v1/auth/verify").param("token", newToken))
                .andExpect(status().isOk());


        mockMvc.perform(get("/api/v1/auth/verify").param("token", oldToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oldAccessTokenRejectedAfterPasswordReset() throws Exception {
        registerAndActivate();
        String oldToken = loginAndGetToken(PASSWORD);


        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        resetPasswordAndConfirm();


        assertThat(user(EMAIL).getPasswordVersion()).isEqualTo(1);


        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden());


        String newToken = loginAndGetToken(NEW_PASSWORD);
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void oldRefreshTokenRejectedAfterPasswordReset() throws Exception {
        registerAndActivate();
        String oldRefreshToken = loginAndGetRefreshToken(PASSWORD);

        resetPasswordAndConfirm();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oldTokensRejectedAfterChangePassword() throws Exception {
        registerAndActivate();
        String accessToken = loginAndGetToken(PASSWORD);
        String refreshToken = loginAndGetRefreshToken(PASSWORD);

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + PASSWORD
                                + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());


        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendVerificationForVerifiedUserDoesNotCreateNewToken() throws Exception {
        registerAndActivate();

        long countBefore = verificationTokenRepository.findByUserId(user(EMAIL).getId()).size();

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());

        assertThat(verificationTokenRepository.findByUserId(user(EMAIL).getId()).size())
                .isEqualTo(countBefore);
    }


    private String loginAndGetToken(String password) throws Exception {
        return loginAndGet(password, "accessToken");
    }

    private String loginAndGetRefreshToken(String password) throws Exception {
        return loginAndGet(password, "refreshToken");
    }

    private String loginAndGet(String password, String field) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(field).asText();
    }

    private void resetPasswordAndConfirm() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\"}"))
                .andExpect(status().isOk());

        String resetToken = resetTokenRepository.findByUserId(user(EMAIL).getId()).get(0).getToken();

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + resetToken + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private User createUser() {
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), NAME);
        return userRepository.save(user);
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "password", PASSWORD,
                                "fullName", NAME))))
                .andExpect(status().isCreated());
    }

    private void registerAndActivate() throws Exception {
        registerUser();
        User user = user(EMAIL);
        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
