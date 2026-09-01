package com.meethybridhub.admin;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final PlatformConfigRepository configRepo;
    private final CommissionRuleRepository commissionRuleRepo;
    private final CommissionEntryRepository commissionEntryRepo;
    private final DisputeRepository disputeRepo;
    private final DisputeMessageRepository disputeMessageRepo;
    private final SalesSnapshotRepository salesSnapshotRepo;

    public AdminService(PlatformConfigRepository configRepo,
                         CommissionRuleRepository commissionRuleRepo,
                         CommissionEntryRepository commissionEntryRepo,
                         DisputeRepository disputeRepo,
                         DisputeMessageRepository disputeMessageRepo,
                         SalesSnapshotRepository salesSnapshotRepo) {
        this.configRepo = configRepo;
        this.commissionRuleRepo = commissionRuleRepo;
        this.commissionEntryRepo = commissionEntryRepo;
        this.disputeRepo = disputeRepo;
        this.disputeMessageRepo = disputeMessageRepo;
        this.salesSnapshotRepo = salesSnapshotRepo;
    }

    // ─── Platform Config ────────────────────────────────────────────

    public PlatformConfig getConfig(String key) {
        return configRepo.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found: " + key));
    }

    public Map<String, String> getAllConfig() {
        Map<String, String> map = new LinkedHashMap<>();
        configRepo.findAll().forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    public PlatformConfig setConfig(String key, String value, String description, Long updatedBy) {
        PlatformConfig config = configRepo.findByConfigKey(key)
                .orElse(new PlatformConfig(key, value, description));
        config.setConfigValue(value);
        if (description != null) config = new PlatformConfig(key, value, description);
        config.setUpdatedBy(updatedBy);
        PlatformConfig saved = configRepo.save(config);
        log.info("Platform config set: {} = {}", key, value);
        return saved;
    }

    // ─── Commission Rules ───────────────────────────────────────────

    public CommissionRule createCommissionRule(Long storeId, CommissionRuleType ruleType,
                                                BigDecimal rate, String currency,
                                                BigDecimal minOrder, BigDecimal maxOrder) {
        CommissionRule rule = new CommissionRule(storeId, ruleType, rate, currency, minOrder, maxOrder);
        CommissionRule saved = commissionRuleRepo.save(rule);
        log.info("Commission rule created: {} for store {}", saved.getId(), storeId);
        return saved;
    }

    public List<CommissionRule> listCommissionRules(Long storeId) {
        return commissionRuleRepo.findActiveForStore(storeId);
    }

    public CommissionRule updateCommissionRule(Long ruleId, BigDecimal rate, boolean active) {
        CommissionRule rule = commissionRuleRepo.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule not found: " + ruleId));
        if (rate != null) rule.setRate(rate);
        rule.setActive(active);
        return commissionRuleRepo.save(rule);
    }

    // ─── Commission Entries ─────────────────────────────────────────

    public CommissionEntry calculateCommission(Long storeId, Long orderId, BigDecimal orderAmount) {
        CommissionRule rule = commissionRuleRepo.findActiveForStore(storeId).stream()
                .filter(r -> r.getRuleType() == CommissionRuleType.PERCENTAGE
                        || (r.getMinOrder() != null && orderAmount.compareTo(r.getMinOrder()) >= 0))
                .findFirst()
                .orElse(null);

        BigDecimal commissionAmount;
        if (rule != null) {
            commissionAmount = rule.calculate(orderAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            commissionAmount = BigDecimal.ZERO;
        }

        CommissionEntry entry = new CommissionEntry(
                storeId, orderId, rule != null ? rule.getId() : null,
                orderAmount, commissionAmount, "NGN");
        CommissionEntry saved = commissionEntryRepo.save(entry);
        log.info("Commission calculated: {} for order {} — {}", saved.getId(), orderId, commissionAmount);
        return saved;
    }

    public List<CommissionEntry> listCommissions(Long storeId) {
        return commissionEntryRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public Map<String, Object> getCommissionSummary(Long storeId) {
        BigDecimal pending = commissionEntryRepo.sumCommissionByStoreAndStatus(storeId, CommissionStatus.PENDING);
        BigDecimal paid = commissionEntryRepo.sumCommissionByStoreAndStatus(storeId, CommissionStatus.PAID);
        long total = commissionEntryRepo.countByStoreId(storeId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingAmount", pending);
        summary.put("paidAmount", paid);
        summary.put("totalEntries", total);
        return summary;
    }

    // ─── Disputes ───────────────────────────────────────────────────

    public Dispute createDispute(Long storeId, Long orderId, Long commissionId,
                                  Long filedById, DisputeType type,
                                  String subject, String description,
                                  DisputePriority priority) {
        Dispute dispute = new Dispute(storeId, orderId, commissionId, filedById,
                type, subject, description, priority);
        Dispute saved = disputeRepo.save(dispute);
        log.info("Dispute created: {} — {} by user {}", saved.getId(), subject, filedById);
        return saved;
    }

    public Dispute getDispute(Long disputeId) {
        return disputeRepo.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
    }

    public List<Dispute> listDisputes(Long storeId, String status) {
        if (status != null && !status.isBlank()) {
            DisputeStatus ds;
            try {
                ds = DisputeStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown status: " + status);
            }
            return disputeRepo.findByStatus(ds);
        }
        return disputeRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public Dispute updateDisputeStatus(Long disputeId, DisputeStatus newStatus,
                                        String resolution, Long assignedToId) {
        Dispute dispute = getDispute(disputeId);
        dispute.setStatus(newStatus);
        if (resolution != null) dispute.setResolution(resolution);
        if (assignedToId != null) dispute.setAssignedToId(assignedToId);
        Dispute saved = disputeRepo.save(dispute);
        log.info("Dispute {} status: {} → {}", disputeId, dispute.getStatus(), newStatus);
        return saved;
    }

    public DisputeMessage addDisputeMessage(Long disputeId, Long senderId, String message) {
        Dispute dispute = getDispute(disputeId);
        DisputeMessage msg = new DisputeMessage(dispute, senderId, message.trim());
        DisputeMessage saved = disputeMessageRepo.save(msg);

        if (dispute.getStatus() == DisputeStatus.OPEN) {
            dispute.setStatus(DisputeStatus.IN_REVIEW);
            disputeRepo.save(dispute);
        }

        return saved;
    }

    public List<DisputeMessage> listDisputeMessages(Long disputeId) {
        return disputeMessageRepo.findByDisputeIdOrderByCreatedAtAsc(disputeId);
    }

    // ─── Analytics ──────────────────────────────────────────────────

    public Map<String, Object> getStoreAnalytics(Long storeId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);

        BigDecimal revenue = salesSnapshotRepo.sumRevenueByStoreAndDateRange(storeId, from, to);
        long orders = salesSnapshotRepo.sumOrdersByStoreAndDateRange(storeId, from, to);
        List<SalesSnapshot> daily = salesSnapshotRepo.findByStoreIdAndDateRange(storeId, from, to);

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("period", Map.of("from", from.toString(), "to", to.toString(), "days", days));
        analytics.put("totalRevenue", revenue);
        analytics.put("totalOrders", orders);
        analytics.put("averageOrderValue", orders > 0
                ? revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        analytics.put("daily", daily.stream().map(s -> Map.of(
                "date", s.getSnapshotDate().toString(),
                "orders", s.getOrderCount(),
                "revenue", s.getRevenue(),
                "commission", s.getCommission()
        )).toList());

        return analytics;
    }

    public Map<String, Object> getPlatformAnalytics() {
        long openDisputes = disputeRepo.countByStatus(DisputeStatus.OPEN);
        long inReviewDisputes = disputeRepo.countByStatus(DisputeStatus.IN_REVIEW);
        long totalDisputes = openDisputes + inReviewDisputes
                + disputeRepo.countByStatus(DisputeStatus.RESOLVED)
                + disputeRepo.countByStatus(DisputeStatus.CLOSED)
                + disputeRepo.countByStatus(DisputeStatus.ESCALATED);

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("disputes", Map.of(
                "open", openDisputes,
                "inReview", inReviewDisputes,
                "total", totalDisputes));
        return analytics;
    }
}
