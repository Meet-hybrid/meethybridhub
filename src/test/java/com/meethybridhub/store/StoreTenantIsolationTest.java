package com.meethybridhub.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the multi-tenancy middleware (Hybrid's Card 1):
 *   - StoreFilter resolves the tenant from the X-Store-Id header / subdomain
 *   - Store creation grants the STORE_OWNER role
 *   - Cross-store data isolation (a store can never read another store's data)
 *   - Ownership enforcement (a store owner cannot act on someone else's store)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreTenantIsolationTest {

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
    void storeCreationGrantsStoreOwnerRole() throws Exception {
        String token = registerAndGetToken("owner-a@example.com", "Owner A");

        mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Divine Signature\", \"description\": \"Fashion store\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("divine-signature"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User owner = userRepository.findByEmail("owner-a@example.com").orElseThrow();
        assertThat(owner.hasRole("STORE_OWNER")).isTrue();
    }

    @Test
    void tenantResolvedFromXStoreIdHeader() throws Exception {
        String token = registerAndGetToken("owner-b@example.com", "Owner B");
        long storeId = createStore(token, "Shop B");

        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId))
                .andExpect(jsonPath("$.name").value("Shop B"));
    }

    @Test
    void tenantResolvedFromJwtClaimAfterLogin() throws Exception {
        // A token issued at login (after the store exists) carries a storeId
        // claim, so store-owner dashboards need no X-Store-Id header or subdomain.
        String registerToken = registerAndGetToken("claim@example.com", "Claim Owner");
        long storeId = createStore(registerToken, "Claim Shop");

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"claim@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginBody).get("accessToken").asText();

        // No X-Store-Id header, bare Host — the claim alone resolves the tenant.
        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId))
                .andExpect(jsonPath("$.name").value("Claim Shop"));
    }

    @Test
    void explicitTenantSourcesTakePrecedenceOverJwtClaim() throws Exception {
        // Owner A owns store "Prec A" (slug prec-a); owner B owns "Prec B" (slug prec-b)
        String tokenA = registerAndGetToken("prec-a@example.com", "Prec A");
        createStore(tokenA, "Prec A");

        String tokenB = registerAndGetToken("prec-b@example.com", "Prec B");
        long storeB = createStore(tokenB, "Prec B");

        // Login as owner A -> token carries a claim for store A
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"prec-a@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginBody).get("accessToken").asText();

        // (1) Subdomain of store B beats the claim -> tenant is B -> not owner -> 403
        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Host", "prec-b.meethybridhub.com"))
                .andExpect(status().isForbidden());

        // (2) X-Store-Id of store B beats the claim -> 403 for owner A
        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Store-Id", storeB))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenPicksUpNewlyCreatedStore() throws Exception {
        // Login BEFORE creating the store -> no claim in the original token
        String registerToken = registerAndGetToken("refresh-claim@example.com", "Refresh Claim");
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"refresh-claim@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        // Create the store, then refresh -> the new access token carries the claim
        long storeId = createStore(registerToken, "Refresh Claim Shop");

        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(refreshBody).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId));
    }

    @Test
    void tenantResolvedFromSubdomain() throws Exception {
        String token = registerAndGetToken("owner-c@example.com", "Owner C");
        long storeId = createStore(token, "Subdomain Shop"); // slug: subdomain-shop

        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + token)
                        .header("Host", "subdomain-shop.meethybridhub.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId));
    }

    @Test
    void domainsAreIsolatedBetweenStores() throws Exception {
        // Store A with its own domain
        String tokenA = registerAndGetToken("iso-a@example.com", "Isolation A");
        long storeA = createStore(tokenA, "Isolation A");
        addDomain(tokenA, storeA, "store-a.example.com");

        // Store B with its own domain
        String tokenB = registerAndGetToken("iso-b@example.com", "Isolation B");
        long storeB = createStore(tokenB, "Isolation B");
        addDomain(tokenB, storeB, "store-b.example.com");

        // Tenant A sees only A's domain
        String bodyA = mockMvc.perform(get("/api/v1/stores/me/domains")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Store-Id", storeA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(bodyA)).hasSize(1);
        assertThat(objectMapper.readTree(bodyA).get(0).get("domain").asText()).isEqualTo("store-a.example.com");

        // Tenant B sees only B's domain
        String bodyB = mockMvc.perform(get("/api/v1/stores/me/domains")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Store-Id", storeB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(bodyB)).hasSize(1);
        assertThat(objectMapper.readTree(bodyB).get(0).get("domain").asText()).isEqualTo("store-b.example.com");
    }

    @Test
    void storeOwnerCannotAccessAnotherStore() throws Exception {
        String tokenA = registerAndGetToken("cross-a@example.com", "Cross A");
        long storeA = createStore(tokenA, "Cross A");

        String tokenB = registerAndGetToken("cross-b@example.com", "Cross B");
        createStore(tokenB, "Cross B");

        // Owner B tries to operate on store A -> 403
        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Store-Id", storeA))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTenantContextReturns400() throws Exception {
        String token = registerAndGetToken("no-tenant@example.com", "No Tenant");
        createStore(token, "No Tenant Shop");

        // Authenticated but no X-Store-Id header and bare Host -> no tenant
        mockMvc.perform(get("/api/v1/stores/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Register a user and verify their email, so the returned access token is
     * actually usable (the JWT filter rejects unverified accounts).
     */
    private String registerAndGetToken(String email, String fullName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", fullName))))
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
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
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        long storeId = body.get("id").asLong();

        // Store created -> owner now has a store
        assertThat(storeId).isPositive();
        return storeId;
    }

    private void addDomain(String token, long storeId, String domain) throws Exception {
        mockMvc.perform(post("/api/v1/stores/me/domains")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\": \"" + domain + "\"}"))
                .andExpect(status().isCreated());
    }
}
