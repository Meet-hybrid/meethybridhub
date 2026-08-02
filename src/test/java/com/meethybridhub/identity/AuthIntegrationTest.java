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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication endpoints.
 *
 * Tests the complete authentication flow:
 *   - User registration
 *   - User login
 *   - Token refresh
 *   - Input validation
 *
 * Uses the test profile with H2 in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_NAME = "Test User";

    @Test
    void registerUserSuccessfully() throws Exception {
        Map<String, String> request = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD,
                "fullName", TEST_NAME
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.message").value("Registration successful. Please verify your email."));
    }

    @Test
    void registerWithInvalidEmailFails() throws Exception {
        Map<String, String> request = Map.of(
                "email", "invalid-email",
                "password", TEST_PASSWORD,
                "fullName", TEST_NAME
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithWeakPasswordFails() throws Exception {
        Map<String, String> request = Map.of(
                "email", TEST_EMAIL,
                "password", "weak",  // Too short, no uppercase, no special char
                "fullName", TEST_NAME
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithValidCredentialsAfterRegistrationFailsWithoutVerification() throws Exception {
        // First register
        Map<String, String> registerRequest = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD,
                "fullName", TEST_NAME
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)));

        // Then login - should fail because email is not verified
        Map<String, String> loginRequest = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithInvalidCredentialsFails() throws Exception {
        Map<String, String> request = Map.of(
                "email", TEST_EMAIL,
                "password", "WrongPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenWithValidToken() throws Exception {
        // Register and get tokens
        Map<String, String> registerRequest = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD,
                "fullName", TEST_NAME
        );

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract refresh token from response
        String refreshToken = objectMapper.readTree(registerResponse)
                .get("refreshToken")
                .asText();

        // Refresh token
        Map<String, String> refreshRequest = Map.of(
                "refreshToken", refreshToken
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.message").value("Token refreshed"));
    }

    @Test
    void refreshTokenWithInvalidTokenFails() throws Exception {
        Map<String, String> request = Map.of(
                "refreshToken", "invalid.token.here"
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestPasswordResetForNonExistentUserReturnsOk() throws Exception {
        Map<String, String> request = Map.of(
                "email", "nonexistent@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent if account exists"));
    }
}