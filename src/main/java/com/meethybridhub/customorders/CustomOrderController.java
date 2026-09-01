package com.meethybridhub.customorders;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/custom-orders")
public class CustomOrderController {

    private final CustomOrderService service;
    private final UserService userService;

    public CustomOrderController(CustomOrderService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    // ─── Requests ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestResponse> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRequestRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        Long storeId = TenantContext.requireStoreId();

        CustomOrderRequest request = service.createRequest(
                user, storeId, body.title(), body.description(),
                body.budgetMin(), body.budgetMax(), body.deadline());

        return ResponseEntity.status(HttpStatus.CREATED).body(RequestResponse.from(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestResponse> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(RequestResponse.from(service.getRequest(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RequestResponse>> listRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        Long storeId = TenantContext.getStoreId().orElse(null);

        List<CustomOrderRequest> requests;
        if (storeId != null && (user.hasRole("STORE_OWNER") || user.hasRole("ADMIN"))) {
            requests = service.listRequestsForStore(storeId, status);
        } else {
            requests = service.listRequestsForCustomer(user.getId());
        }

        return ResponseEntity.ok(requests.stream().map(RequestResponse::from).toList());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<RequestResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        CustomOrderRequest updated = service.updateStatus(user, id, body.status());
        return ResponseEntity.ok(RequestResponse.from(updated));
    }

    // ─── Quotes ─────────────────────────────────────────────────────

    @PostMapping("/{requestId}/quotes")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<QuoteResponse> createQuote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody CreateQuoteRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        Quote quote = service.createQuote(
                user, requestId, body.price(), body.currency(),
                body.estimatedDays(), body.terms(), body.expiryDays());

        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponse.from(quote));
    }

    @GetMapping("/{requestId}/quotes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuoteResponse>> listQuotes(@PathVariable Long requestId) {
        return ResponseEntity.ok(
                service.listQuotesForRequest(requestId).stream()
                        .map(QuoteResponse::from).toList());
    }

    @PostMapping("/quotes/{quoteId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuoteResponse> acceptQuote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long quoteId) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(QuoteResponse.from(service.acceptQuote(user, quoteId)));
    }

    @PostMapping("/quotes/{quoteId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuoteResponse> rejectQuote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long quoteId) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(QuoteResponse.from(service.rejectQuote(user, quoteId)));
    }

    // ─── Conversation ───────────────────────────────────────────────

    @PostMapping("/{requestId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody SendMessageRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        CustomOrderConversation msg = service.sendMessage(user, requestId, body.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(msg));
    }

    @GetMapping("/{requestId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> listMessages(@PathVariable Long requestId) {
        return ResponseEntity.ok(
                service.listMessages(requestId).stream()
                        .map(MessageResponse::from).toList());
    }

    // ─── Attachments ────────────────────────────────────────────────

    @PostMapping("/{requestId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttachmentResponse> addAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody AddAttachmentRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        CustomOrderAttachment attachment = service.addAttachment(
                user, requestId, body.fileUrl(), body.fileName(),
                body.fileType(), body.fileSizeBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(attachment));
    }

    @GetMapping("/{requestId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable Long requestId) {
        return ResponseEntity.ok(
                service.listAttachments(requestId).stream()
                        .map(AttachmentResponse::from).toList());
    }

    // ─── Convert to Order ───────────────────────────────────────────

    @PostMapping("/{requestId}/convert")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> convertToOrder(@PathVariable Long requestId) {
        var order = service.convertToOrder(requestId);
        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "message", "Custom order converted to order successfully"));
    }

    // ─── DTOs ───────────────────────────────────────────────────────

    public record CreateRequestRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank String description,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            Instant deadline) {}

    public record UpdateStatusRequest(
            @NotNull CustomOrderStatus status) {}

    public record CreateQuoteRequest(
            @NotNull BigDecimal price,
            String currency,
            Integer estimatedDays,
            String terms,
            int expiryDays) {}

    public record SendMessageRequest(
            @NotBlank String message) {}

    public record AddAttachmentRequest(
            @NotBlank String fileUrl,
            @NotBlank String fileName,
            String fileType,
            Long fileSizeBytes) {}

    public record RequestResponse(
            Long id, Long customerId, Long storeId, String title, String description,
            CustomOrderStatus status, BigDecimal budgetMin, BigDecimal budgetMax,
            Instant deadline, Instant createdAt) {

        public static RequestResponse from(CustomOrderRequest r) {
            return new RequestResponse(
                    r.getId(), r.getCustomerId(), r.getStoreId(),
                    r.getTitle(), r.getDescription(), r.getStatus(),
                    r.getBudgetMin(), r.getBudgetMax(), r.getDeadline(),
                    r.getCreatedAt());
        }
    }

    public record QuoteResponse(
            Long id, Long requestId, BigDecimal price, String currency,
            Integer estimatedDays, String terms, QuoteStatus status,
            Instant expiresAt, Instant createdAt) {

        public static QuoteResponse from(Quote q) {
            return new QuoteResponse(
                    q.getId(), q.getRequestId(), q.getPrice(), q.getCurrency(),
                    q.getEstimatedDays(), q.getTerms(), q.getStatus(),
                    q.getExpiresAt(), q.getCreatedAt());
        }
    }

    public record MessageResponse(
            Long id, Long senderId, String message, Instant createdAt) {

        public static MessageResponse from(CustomOrderConversation m) {
            return new MessageResponse(m.getId(), m.getSenderId(), m.getMessage(), m.getCreatedAt());
        }
    }

    public record AttachmentResponse(
            Long id, Long uploaderId, String fileUrl, String fileName,
            String fileType, Long fileSizeBytes, Instant createdAt) {

        public static AttachmentResponse from(CustomOrderAttachment a) {
            return new AttachmentResponse(
                    a.getId(), a.getUploaderId(), a.getFileUrl(), a.getFileName(),
                    a.getFileType(), a.getFileSizeBytes(), a.getCreatedAt());
        }
    }
}
