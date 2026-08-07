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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the security audit trail: security-relevant events
 * (registration, verification, login success/failure, admin role & status
 * changes) land in {@code audit_log} with the actor, description and IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void registrationEmitsAuditEvent() throws Exception {
        register("audit-register@example.com", "Audit Register");

        AuditLog row = findByEvent(AuditEventType.REGISTER);
        assertThat(row.getDescription()).contains("audit-register@example.com");
        assertThat(row.getUserId())
                .isEqualTo(userRepository.findByEmail("audit-register@example.com").orElseThrow().getId());
    }

    @Test
    void emailVerificationEmitsAuditEvent() throws Exception {
        register("audit-verify@example.com", "Audit Verify");
        verifyEmail("audit-verify@example.com");

        assertThat(findByEvent(AuditEventType.EMAIL_VERIFIED).getUserId())
                .isEqualTo(userRepository.findByEmail("audit-verify@example.com").orElseThrow().getId());
    }

    @Test
    void loginSuccessAndFailureEmitAuditEvents() throws Exception {
        String email = "audit-login@example.com";
        register(email, "Audit Login");
        verifyEmail(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        AuditLog success = findByEvent(AuditEventType.LOGIN_SUCCESS);
        assertThat(success.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(success.getUserId())
                .isEqualTo(userRepository.findByEmail(email).orElseThrow().getId());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"WrongPassword1!\"}"))
                .andExpect(status().isUnauthorized());

        AuditLog failure = findByEvent(AuditEventType.LOGIN_FAILED);
        assertThat(failure.getDescription()).contains(email);
        assertThat(failure.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(failure.getUserId()).isNull();
    }

    @Test
    void adminRoleAndStatusChangesEmitAuditEvents() throws Exception {
        String adminToken = registerAndPromoteToAdmin("audit-admin@example.com", "Audit Admin");
        String targetEmail = "audit-target@example.com";
        register(targetEmail, "Audit Target");
        Long targetId = userRepository.findByEmail(targetEmail).orElseThrow().getId();

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", targetId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": \"STORE_OWNER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/users/{id}/status", targetId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isOk());

        Long adminId = userRepository.findByEmail("audit-admin@example.com").orElseThrow().getId();
        assertThat(findByEvent(AuditEventType.ROLES_UPDATED).getUserId()).isEqualTo(adminId);
        // Admin actions carry the acting admin's IP
        assertThat(findByEvent(AuditEventType.ROLES_UPDATED).getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(findByEvent(AuditEventType.USER_STATUS_UPDATED).getUserId()).isEqualTo(adminId);
    }

    @Test
    void adminDeletionEmitsAuditEvent() throws Exception {
        String adminToken = registerAndPromoteToAdmin("audit-deleter@example.com", "Audit Deleter");
        String targetEmail = "audit-deleted@example.com";
        register(targetEmail, "Audit Deleted");
        Long targetId = userRepository.findByEmail(targetEmail).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/admin/users/{id}", targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Long adminId = userRepository.findByEmail("audit-deleter@example.com").orElseThrow().getId();
        assertThat(findByEvent(AuditEventType.USER_DELETED).getUserId()).isEqualTo(adminId);
        assertThat(findByEvent(AuditEventType.USER_DELETED).getDescription()).contains(String.valueOf(targetId));
    }

    @Test
    void passwordChangeEmitsAuditEvent() throws Exception {
        String email = "audit-pw@example.com";
        String token = register(email, "Audit Pw");
        verifyEmail(email);

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + PASSWORD + "\", \"newPassword\": \"NewPassword456!\"}"))
                .andExpect(status().isOk());

        AuditLog row = findByEvent(AuditEventType.PASSWORD_CHANGED);
        assertThat(row.getUserId())
                .isEqualTo(userRepository.findByEmail(email).orElseThrow().getId());
    }

    @Test
    void passwordResetConfirmEmitsAuditEvent() throws Exception {
        String email = "audit-reset@example.com";
        register(email, "Audit Reset");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\"}"))
                .andExpect(status().isOk());

        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        String resetToken = passwordResetTokenRepository.findByUserId(userId).get(0).getToken();

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + resetToken + "\", \"newPassword\": \"NewPassword456!\"}"))
                .andExpect(status().isOk());

        assertThat(findByEvent(AuditEventType.PASSWORD_RESET_CONFIRMED).getUserId()).isEqualTo(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** The latest audit row for the given event type (rows are ordered by id). */
    private AuditLog findByEvent(AuditEventType eventType) {
        List<AuditLog> rows = auditLogRepository.findAll().stream()
                .filter(r -> r.getEventType() == eventType)
                .toList();
        assertThat(rows).isNotEmpty();
        return rows.get(rows.size() - 1);
    }

    private String register(String email, String fullName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", fullName))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private void verifyEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = tokenRepository.findByUserId(user.getId()).get(0).getToken();
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk());
    }

    private String registerAndPromoteToAdmin(String email, String fullName) throws Exception {
        String token = register(email, fullName);
        verifyEmail(email);

        // Promote to ADMIN in the database; the JWT filter re-loads authorities
        // from the DB on every request, so the next call already has ADMIN.
        User user = userRepository.findByEmail(email).orElseThrow();
        user.addRole("ADMIN");
        userRepository.save(user);
        return token;
    }
}
