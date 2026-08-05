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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for role-based access control (Hybrid's Card 3):
 *   - ADMIN can list/manage users (roles, status, soft delete)
 *   - Role strings are validated and normalized
 *   - Non-admins are denied admin endpoints (403)
 *   - Authenticated-only endpoints reject anonymous callers
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserManagementIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Test
    void adminCanListAndManageUsers() throws Exception {
        String adminToken = registerAndPromoteToAdmin("root@example.com", "Root Admin");
        String customerEmail = "customer@example.com";
        registerAndGetToken(customerEmail, "Customer One");

        Long customerId = userRepository.findByEmail(customerEmail).orElseThrow().getId();

        // List users
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'customer@example.com')]").exists());

        // Filter by role
        mockMvc.perform(get("/api/v1/admin/users").param("role", "ADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'root@example.com')]").exists());

        // Update roles: messy input is normalized
        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": \"customer, store_owner, CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("CUSTOMER,STORE_OWNER"));

        // Invalid role rejected
        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": \"SUPERHERO\"}"))
                .andExpect(status().isBadRequest());

        // Update status
        mockMvc.perform(put("/api/v1/admin/users/{id}/status", customerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Get user detail
        mockMvc.perform(get("/api/v1/admin/users/{id}", customerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customerEmail));

        // Soft delete
        mockMvc.perform(delete("/api/v1/admin/users/{id}", customerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User deleted = userRepository.findById(customerId).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(User.UserStatus.DELETED);
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        String customerToken = registerAndGetToken("plain@example.com", "Plain Customer");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Register a user and verify their email, so the returned access token is
     * actually usable (the JWT filter rejects unverified accounts).
     */
    private String registerAndGetToken(String email, String fullName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", fullName))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).get("accessToken").asText();
        verifyEmail(email);
        return accessToken;
    }

    private String registerAndPromoteToAdmin(String email, String fullName) throws Exception {
        String token = registerAndGetToken(email, fullName);

        // Promote to ADMIN in the database; the JWT filter re-loads authorities
        // from the DB on every request, so the next call already has ADMIN.
        User user = userRepository.findByEmail(email).orElseThrow();
        user.addRole("ADMIN");
        userRepository.save(user);
        return token;
    }

    private void verifyEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk());
    }
}
