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
 * Integration tests for store settings & branding (Phase 3, Week 4):
 *   - GET /stores/me/settings returns defaults (created lazily)
 *   - PUT /stores/me/settings updates branding and records an audit event
 *   - Tenant isolation: a store owner cannot touch another store's settings
 *   - Validation: malformed colors are rejected with 400
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreSettingsIntegrationTest {

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
    private StoreSettingsRepository storeSettingsRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void getSettingsReturnsLazilyCreatedDefaults() throws Exception {
        String token = registerAndGetToken("settings-a@example.com", "Settings A");
        long storeId = createStore(token, "Settings A Shop");

        mockMvc.perform(get("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.primaryColor").value("#111111"))
                .andExpect(jsonPath("$.accentColor").value("#0d9488"))
                .andExpect(jsonPath("$.theme").value("LIGHT"));

        // A settings row was lazily created
        assertThat(storeSettingsRepository.findByStoreId(storeId)).isPresent();
    }

    @Test
    void updateSettingsPersistsAndAudits() throws Exception {
        String token = registerAndGetToken("settings-b@example.com", "Settings B");
        long storeId = createStore(token, "Settings B Shop");

        mockMvc.perform(put("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "logoUrl", "https://cdn.example.com/logo.png",
                                "primaryColor", "#FF5733",
                                "accentColor", "#33FF57",
                                "theme", "DARK",
                                "tagline", "Fashion for everyone",
                                "contactEmail", "hello@settings-b-shop.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.example.com/logo.png"))
                .andExpect(jsonPath("$.primaryColor").value("#FF5733"))
                .andExpect(jsonPath("$.accentColor").value("#33FF57"))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.tagline").value("Fashion for everyone"))
                .andExpect(jsonPath("$.contactEmail").value("hello@settings-b-shop.com"));

        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId).orElseThrow();
        assertThat(settings.getTheme()).isEqualTo(StoreTheme.DARK);
        assertThat(settings.getPrimaryColor()).isEqualTo("#FF5733");

        boolean audited = auditLogRepository.findAll().stream()
                .anyMatch(r -> r.getEventType() == AuditEventType.STORE_SETTINGS_UPDATED
                        && r.getDescription().contains("settings-b-shop"));
        assertThat(audited).isTrue();
    }

    @Test
    void partialUpdateKeepsUnchangedFields() throws Exception {
        String token = registerAndGetToken("settings-c@example.com", "Settings C");
        long storeId = createStore(token, "Settings C Shop");

        // First set the tagline only
        mockMvc.perform(put("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagline\": \"Only this changes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Only this changes"))
                .andExpect(jsonPath("$.theme").value("LIGHT"));

        // Then change the theme; tagline must survive
        mockMvc.perform(put("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\": \"DARK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Only this changes"))
                .andExpect(jsonPath("$.theme").value("DARK"));
    }

    @Test
    void invalidColorReturns400() throws Exception {
        String token = registerAndGetToken("settings-d@example.com", "Settings D");
        long storeId = createStore(token, "Settings D Shop");

        mockMvc.perform(put("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryColor\": \"not-a-color\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void storeOwnerCannotTouchAnotherStoresSettings() throws Exception {
        String tokenA = registerAndGetToken("settings-e@example.com", "Settings E");
        long storeA = createStore(tokenA, "Settings E Shop");

        // Owner A materializes their settings row with the default theme
        mockMvc.perform(get("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Store-Id", storeA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("LIGHT"));

        String tokenB = registerAndGetToken("settings-f@example.com", "Settings F");
        createStore(tokenB, "Settings F Shop");

        // Owner B tries to read/store A's settings via A's tenant context -> 403
        mockMvc.perform(get("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Store-Id", storeA))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Store-Id", storeA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\": \"DARK\"}"))
                .andExpect(status().isForbidden());

        // A's settings were never touched
        StoreSettings settings = storeSettingsRepository.findByStoreId(storeA).orElseThrow();
        assertThat(settings.getTheme()).isEqualTo(StoreTheme.LIGHT);
    }

    @Test
    void customerWithoutStoreIsDenied() throws Exception {
        String token = registerAndGetToken("settings-g@example.com", "Settings G");

        mockMvc.perform(get("/api/v1/stores/me/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
