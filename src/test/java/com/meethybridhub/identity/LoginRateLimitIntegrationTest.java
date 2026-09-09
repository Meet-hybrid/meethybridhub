package com.meethybridhub.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginRateLimitIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Test
    void failedAndSuccessfulAttemptsAreRecorded() throws Exception {
        registerAndActivate("tracked@example.com");


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"tracked@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        Instant windowStart = Instant.now().minus(15, ChronoUnit.MINUTES);
        assertThat(loginAttemptRepository.countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
                "tracked@example.com", LoginAttempt.Purpose.LOGIN, false, windowStart)).isEqualTo(1);


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"tracked@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        assertThat(loginAttemptRepository.countByEmailAndPurposeAndSuccessAndCreatedAtAfter(
                "tracked@example.com", LoginAttempt.Purpose.LOGIN, true, windowStart)).isEqualTo(1);
    }

    @Test
    void accountIsLockedAfterTooManyFailedAttempts() throws Exception {
        registerAndActivate("locked@example.com");


        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"locked@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"locked@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginResetsFailedAttemptCounter() throws Exception {
        registerAndActivate("reset@example.com");


        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"reset@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"reset@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());


        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"reset@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"reset@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimitResponseIncludesRetryAfterHeader() throws Exception {
        registerAndActivate("retry@example.com");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"retry@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"retry@example.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"));
    }

    @Test
    void spoofedForwardedHeaderDoesNotBypassRateLimit() throws Exception {


        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"spoof-" + i + "@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"spoof-5@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void forwardedHeaderIsHonoredWhenTrusted() throws Exception {


        ReflectionTestUtils.setField(clientIpResolver, "trustForwardedHeader", true);
        try {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                                .header("X-Forwarded-For", "203.0.113.50")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"trusted-" + i + "@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                        .andExpect(status().isUnauthorized());
            }


            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113.50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"trusted-5@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isTooManyRequests());
        } finally {
            ReflectionTestUtils.setField(clientIpResolver, "trustForwardedHeader", false);
        }
    }

    @Test
    void ipIsRateLimitedAfterExcessiveAttempts() throws Exception {


        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"ip-" + i + "@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());
        }


        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ip-5@example.com\", \"password\": \"" + WRONG_PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private void registerAndActivate(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", "Rate Limit User"))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
