package com.meethybridhub.admin;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminService service;
    private final UserService userService;

    public AdminAnalyticsController(AdminService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    // ─── Platform Config ────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getAllConfig() {
        return ResponseEntity.ok(service.getAllConfig());
    }

    @GetMapping("/config/{key}")
    public ResponseEntity<PlatformConfig> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(service.getConfig(key));
    }

    @PutMapping("/config/{key}")
    public ResponseEntity<PlatformConfig> setConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String key,
            @Valid @RequestBody SetConfigRequest body) {

        User admin = userService.getUserByEmail(userDetails.getUsername());
        PlatformConfig config = service.setConfig(key, body.value(), body.description(), admin.getId());
        return ResponseEntity.ok(config);
    }

    // ─── Commission Rules ───────────────────────────────────────────

    @PostMapping("/commissions/rules")
    public ResponseEntity<CommissionRule> createCommissionRule(
            @Valid @RequestBody CreateCommissionRuleRequest body) {
        CommissionRule rule = service.createCommissionRule(
                body.storeId(), body.ruleType(), body.rate(),
                body.currency(), body.minOrder(), body.maxOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping("/commissions/rules")
    public ResponseEntity<List<CommissionRule>> listCommissionRules(
            @RequestParam Long storeId) {
        return ResponseEntity.ok(service.listCommissionRules(storeId));
    }

    @PutMapping("/commissions/rules/{ruleId}")
    public ResponseEntity<CommissionRule> updateCommissionRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateCommissionRuleRequest body) {
        return ResponseEntity.ok(service.updateCommissionRule(ruleId, body.rate(), body.active()));
    }

    // ─── Commission Entries ─────────────────────────────────────────

    @GetMapping("/commissions/entries")
    public ResponseEntity<List<CommissionEntry>> listCommissions(
            @RequestParam Long storeId) {
        return ResponseEntity.ok(service.listCommissions(storeId));
    }

    @GetMapping("/commissions/summary")
    public ResponseEntity<Map<String, Object>> commissionSummary(
            @RequestParam Long storeId) {
        return ResponseEntity.ok(service.getCommissionSummary(storeId));
    }

    // ─── Disputes ───────────────────────────────────────────────────

    @PostMapping("/disputes")
    public ResponseEntity<Dispute> createDispute(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDisputeRequest body) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        Dispute dispute = service.createDispute(
                body.storeId(), body.orderId(), body.commissionId(),
                user.getId(), body.disputeType(), body.subject(),
                body.description(), body.priority());
        return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
    }

    @GetMapping("/disputes/{id}")
    public ResponseEntity<Dispute> getDispute(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDispute(id));
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<Dispute>> listDisputes(
            @RequestParam Long storeId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.listDisputes(storeId, status));
    }

    @PutMapping("/disputes/{id}/status")
    public ResponseEntity<Dispute> updateDisputeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDisputeStatusRequest body) {
        return ResponseEntity.ok(service.updateDisputeStatus(
                id, body.status(), body.resolution(), body.assignedToId()));
    }

    @PostMapping("/disputes/{id}/messages")
    public ResponseEntity<DisputeMessage> addDisputeMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest body) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addDisputeMessage(id, user.getId(), body.message()));
    }

    @GetMapping("/disputes/{id}/messages")
    public ResponseEntity<List<DisputeMessage>> listDisputeMessages(@PathVariable Long id) {
        return ResponseEntity.ok(service.listDisputeMessages(id));
    }

    // ─── Analytics ──────────────────────────────────────────────────

    @GetMapping("/analytics/stores/{storeId}")
    public ResponseEntity<Map<String, Object>> storeAnalytics(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(service.getStoreAnalytics(storeId, days));
    }

    @GetMapping("/analytics/platform")
    public ResponseEntity<Map<String, Object>> platformAnalytics() {
        return ResponseEntity.ok(service.getPlatformAnalytics());
    }

    // ─── DTOs ───────────────────────────────────────────────────────

    public record SetConfigRequest(
            @NotBlank String value,
            String description) {}

    public record CreateCommissionRuleRequest(
            Long storeId,
            @NotNull CommissionRuleType ruleType,
            @NotNull BigDecimal rate,
            String currency,
            BigDecimal minOrder,
            BigDecimal maxOrder) {}

    public record UpdateCommissionRuleRequest(
            BigDecimal rate,
            boolean active) {}

    public record CreateDisputeRequest(
            Long storeId,
            Long orderId,
            Long commissionId,
            @NotNull DisputeType disputeType,
            @NotBlank String subject,
            @NotBlank String description,
            DisputePriority priority) {}

    public record UpdateDisputeStatusRequest(
            @NotNull DisputeStatus status,
            String resolution,
            Long assignedToId) {}

    public record SendMessageRequest(
            @NotBlank String message) {}
}
