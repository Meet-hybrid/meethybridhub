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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code DELETE /api/v1/users/me}:
 *   - the password is verified (without re-hashing) before deletion
 *   - a wrong password is rejected with 400
 *   - a deleted account's token stops working
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountDeletionIntegrationTest {

    private static final String EMAIL = "delete-me@example.com";
    private static final String PASSWORD = "TestPassword123!";
    private static final String NAME = "Delete Me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deleteAccountWithCorrectPasswordDeletesAccount() throws Exception {
        String accessToken = registerVerifyAndLogin();

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        User deleted = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(User.UserStatus.DELETED);

        // A soft-deleted account's token no longer authenticates
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAccountWithWrongPasswordFails() throws Exception {
        String accessToken = registerVerifyAndLogin();

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"WrongPassword123!\"}"))
                .andExpect(status().isBadRequest());

        // Account untouched
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String registerVerifyAndLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "password", PASSWORD,
                                "fullName", NAME))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(loginBody).get("accessToken").asText();
    }
}
