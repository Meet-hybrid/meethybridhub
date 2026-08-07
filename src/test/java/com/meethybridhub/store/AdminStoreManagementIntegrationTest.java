package com.meethybridhub.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogRepository;
import com.meethybridhub.identity.EmailVerificationTokenRepository;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for admin store management (remaining Card 3 scope):
 *   - ADMIN can list stores (all or by status) and change store status
 *   - Status changes are recorded in the audit trail
 *   - Non-admins are denied (403), unknown stores 404, invalid status 400
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminStoreManagementIntegrationTest {

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
    private StoreRepository storeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void adminCanListAndUpdateStores() throws Exception {
        String adminToken = registerAndPromoteToAdmin("admin-stores@example.com", "Admin Stores");
        String ownerToken = registerAndGetToken("store-owner@example.com", "Store Owner");
        long storeId = createStore(ownerToken, "Admin Managed Shop");

        // List all stores
        mockMvc.perform(get("/api/v1/admin/stores")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Admin Managed Shop')]").exists());

        // Filter by status
        mockMvc.perform(get("/api/v1/admin/stores").param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + storeId + ")]").exists());

        // Update status
        mockMvc.perform(put("/api/v1/admin/stores/{id}/status", storeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Store creation itself is audited
        boolean createdAudited = auditLogRepository.findAll().stream()
                .anyMatch(r -> r.getEventType() == AuditEventType.STORE_CREATED
                        && r.getDescription().contains("admin-managed-shop"));
        assertThat(createdAudited).isTrue();

        Store store = storeRepository.findById(storeId).orElseThrow();
        assertThat(store.getStatus()).isEqualTo(StoreStatus.SUSPENDED);

        // The change is attributable in the audit trail (description uses the slug)
        boolean audited = auditLogRepository.findAll().stream()
                .anyMatch(r -> r.getEventType() == AuditEventType.STORE_STATUS_UPDATED
                        && r.getDescription().contains("admin-managed-shop")
                        && r.getDescription().contains("SUSPENDED"));
        assertThat(audited).isTrue();
    }

    @Test
    void nonAdminCannotManageStores() throws Exception {
        String ownerToken = registerAndGetToken("owner-only@example.com", "Owner Only");
        long storeId = createStore(ownerToken, "Owner Shop");

        mockMvc.perform(get("/api/v1/admin/stores")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/stores/{id}/status", storeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownStatusFilterReturns400() throws Exception {
        String adminToken = registerAndPromoteToAdmin("admin-filter@example.com", "Admin Filter");

        mockMvc.perform(get("/api/v1/admin/stores").param("status", "BOGUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownStoreReturns404() throws Exception {
        String adminToken = registerAndPromoteToAdmin("admin-404@example.com", "Admin 404");

        mockMvc.perform(put("/api/v1/admin/stores/{id}/status", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        String adminToken = registerAndPromoteToAdmin("admin-invalid@example.com", "Admin Invalid");
        String ownerToken = registerAndGetToken("invalid-target@example.com", "Invalid Target");
        long storeId = createStore(ownerToken, "Invalid Shop");

        mockMvc.perform(put("/api/v1/admin/stores/{id}/status", storeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    private long createStore(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
