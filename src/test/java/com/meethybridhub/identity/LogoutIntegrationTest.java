package com.meethybridhub.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
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


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LogoutIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void refreshRejectedAfterLogout() throws Exception {
        String email = "logout-a@example.com";
        String refreshToken = registerAndLogin(email);


        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());


        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertThat(revokedTokenRepository.count()).isEqualTo(1);


        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutIsIdempotentAndDoesNotLeakTokenValidity() throws Exception {
        String refreshToken = registerAndLogin("logout-b@example.com");


        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
        assertThat(revokedTokenRepository.count()).isEqualTo(1);


        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"not.a.real.token\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRecordsAuditEvent() throws Exception {
        String email = "logout-c@example.com";
        String refreshToken = registerAndLogin(email);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();
        boolean audited = auditLogRepository.findAll().stream()
                .anyMatch(r -> r.getEventType() == AuditEventType.LOGOUT
                        && user.getId().equals(r.getUserId()));
        assertThat(audited).isTrue();
    }

    @Test
    void accessTokenStillWorksAfterLogout() throws Exception {
        String email = "logout-d@example.com";
        String refreshToken = registerAndLogin(email);
        String accessToken = loginAndGetAccessToken(email);


        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }


    private String registerAndLogin(String email) throws Exception {
        registerAndVerify(email);
        return loginAndGet(email, "refreshToken");
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        return loginAndGet(email, "accessToken");
    }

    private void registerAndVerify(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", "Logout User"))))
                .andExpect(status().isCreated());


        User user = userRepository.findByEmail(email).orElseThrow();
        String verifyToken = verificationTokenRepository.findByUserId(user.getId()).get(0).getToken();
        mockMvc.perform(get("/api/v1/auth/verify").param("token", verifyToken))
                .andExpect(status().isOk());
    }

    private String loginAndGet(String email, String field) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asText();
    }
}
