package com.meethybridhub.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.UnauthorizedException;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public PaymentController(PaymentService paymentService, UserService userService, ObjectMapper objectMapper,
                             @Value("${payment.webhook-secret:dev-webhook-secret}") String webhookSecret) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/orders/{orderId}/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initialize(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitializePaymentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        Payment payment = paymentService.initialize(TenantContext.requireStoreId(), orderId, user,
                request.paymentMethod(), idempotencyKey);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> get(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long paymentId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(PaymentResponse.from(paymentService.get(TenantContext.requireStoreId(), paymentId, user)));
    }

    /** Korapay webhook receiver. Authentication is replaced by HMAC verification. */
    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Korapay-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        verifySignature(signature, rawPayload);
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            JsonNode data = payload.has("data") ? payload.get("data") : payload;
            String transactionId = requiredText(data, "transaction_id", "reference");
            String status = requiredText(data, "status");
            BigDecimal amount = data.get("amount").decimalValue();
            long storeId = data.get("metadata").get("store_id").asLong();
            paymentService.applyWebhook(storeId, transactionId, status, amount, rawPayload);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            if (ex instanceof BadRequestException badRequest) throw badRequest;
            if (ex instanceof UnauthorizedException unauthorized) throw unauthorized;
            throw new BadRequestException("Invalid payment webhook payload");
        }
    }

    private void verifySignature(String signature, String payload) {
        if (signature == null || signature.isBlank()) {
            throw new UnauthorizedException("Missing payment webhook signature");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.trim().getBytes(StandardCharsets.UTF_8))) {
                throw new UnauthorizedException("Invalid payment webhook signature");
            }
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify payment webhook signature", ex);
        }
    }

    private String requiredText(JsonNode node, String... names) {
        for (String name : names) {
            if (node.hasNonNull(name) && !node.get(name).asText().isBlank()) return node.get(name).asText();
        }
        throw new BadRequestException("Payment webhook is missing a required field");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    public record InitializePaymentRequest(@NotNull PaymentMethod paymentMethod) {}

    public record PaymentResponse(Long id, Long storeId, Long orderId, String orderNumber,
                                  PaymentMethod paymentMethod, PaymentStatus status, BigDecimal amount,
                                  String currency, String transactionId, String idempotencyKey) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getStoreId(), payment.getOrderId(), payment.getOrderNumber(),
                    payment.getPaymentMethod(), payment.getStatus(), payment.getAmount(), payment.getCurrency(),
                    payment.getTransactionId(), payment.getIdempotencyKey());
        }
    }
}
