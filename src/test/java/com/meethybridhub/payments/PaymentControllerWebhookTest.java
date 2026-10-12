package com.meethybridhub.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethybridhub.common.api.GlobalExceptionHandler;
import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.identity.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerWebhookTest {

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    private static final String VALID_PAYLOAD = """
            {"transaction_id":"KORAPAY-4","status":"success","amount":20.00,"metadata":{"store_id":7}}""";

    @Mock
    private PaymentService paymentService;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService, userService, new ObjectMapper(), WEBHOOK_SECRET))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void webhookWithValidSignatureReturns200AndForwardsParsedFields() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(VALID_PAYLOAD))
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk());

        verify(paymentService).applyWebhook(eq(7L), eq("KORAPAY-4"), eq("success"),
                argThat((BigDecimal a) -> a.compareTo(new BigDecimal("20.00")) == 0), eq(VALID_PAYLOAD));
    }

    @Test
    void webhookMissingSignatureReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookInvalidSignatureReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", "deadbeef")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookSignatureMismatchWithTamperedPayloadReturns401() throws Exception {
        String signedBody = VALID_PAYLOAD;
        String tamperedBody = VALID_PAYLOAD.replace("20.00", "99.00");

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(signedBody))
                        .content(tamperedBody))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookMissingTransactionIdReturns400() throws Exception {
        String payload = """
                {"status":"success","amount":20.00,"metadata":{"store_id":7}}""";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(payload))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookMissingStatusReturns400() throws Exception {
        String payload = """
                {"transaction_id":"KORAPAY-4","amount":20.00,"metadata":{"store_id":7}}""";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(payload))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookMissingAmountReturns400() throws Exception {
        String payload = """
                {"transaction_id":"KORAPAY-4","status":"success","metadata":{"store_id":7}}""";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(payload))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void webhookServiceAmountMismatchReturns400() throws Exception {
        when(paymentService.applyWebhook(anyLong(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new BadRequestException("Webhook amount does not match payment amount"));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(VALID_PAYLOAD))
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookUnsupportedStatusPropagatesBadRequest() throws Exception {
        String payload = """
                {"transaction_id":"KORAPAY-4","status":"mystery","amount":20.00,"metadata":{"store_id":7}}""";
        when(paymentService.applyWebhook(anyLong(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new BadRequestException("Unsupported payment webhook status: mystery"));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(payload))
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookMalformedJsonReturns400WithoutReachingService() throws Exception {
        String payload = "{not-json";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Korapay-Signature", signature(payload))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).applyWebhook(anyLong(), anyString(), anyString(), any(), anyString());
    }

    private static String signature(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
