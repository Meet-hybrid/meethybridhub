package com.meethybridhub.admin;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private PlatformConfigRepository configRepo;
    @Mock private CommissionRuleRepository commissionRuleRepo;
    @Mock private CommissionEntryRepository commissionEntryRepo;
    @Mock private DisputeRepository disputeRepo;
    @Mock private DisputeMessageRepository disputeMessageRepo;
    @Mock private SalesSnapshotRepository salesSnapshotRepo;

    @InjectMocks private AdminService service;

    // ─── Platform Config ────────────────────────────────────────────

    @Test
    void getConfig_returnsValue() {
        PlatformConfig config = new PlatformConfig("platform.name", "MeethybridHub", "Platform name");
        when(configRepo.findByConfigKey("platform.name")).thenReturn(Optional.of(config));

        PlatformConfig result = service.getConfig("platform.name");
        assertEquals("MeethybridHub", result.getConfigValue());
    }

    @Test
    void getConfig_throwsWhenNotFound() {
        when(configRepo.findByConfigKey("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getConfig("missing"));
    }

    @Test
    void setConfig_createsNew() {
        when(configRepo.findByConfigKey("new.key")).thenReturn(Optional.empty());
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PlatformConfig result = service.setConfig("new.key", "value", "desc", 1L);
        assertEquals("new.key", result.getConfigKey());
        assertEquals("value", result.getConfigValue());
    }

    @Test
    void setConfig_updatesExisting() {
        PlatformConfig existing = new PlatformConfig("key", "old", "desc");
        when(configRepo.findByConfigKey("key")).thenReturn(Optional.of(existing));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PlatformConfig result = service.setConfig("key", "new", null, 1L);
        assertEquals("new", result.getConfigValue());
    }

    // ─── Commission Rules ───────────────────────────────────────────

    @Test
    void createCommissionRule_savesAndReturns() {
        when(commissionRuleRepo.save(any())).thenAnswer(i -> {
            CommissionRule r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        CommissionRule rule = service.createCommissionRule(
                1L, CommissionRuleType.PERCENTAGE, new BigDecimal("2.5"),
                "NGN", null, null);

        assertNotNull(rule);
        assertEquals(CommissionRuleType.PERCENTAGE, rule.getRuleType());
        assertEquals(new BigDecimal("2.5"), rule.getRate());
    }

    @Test
    void commissionRule_calculate_percentage() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.PERCENTAGE,
                new BigDecimal("2.5"), "NGN", null, null);

        BigDecimal commission = rule.calculate(new BigDecimal("10000"));
        assertEquals(new BigDecimal("250.0"), commission);
    }

    @Test
    void commissionRule_calculate_flatFee() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.FLAT_FEE,
                new BigDecimal("500"), "NGN", null, null);

        BigDecimal commission = rule.calculate(new BigDecimal("10000"));
        assertEquals(new BigDecimal("500"), commission);
    }

    @Test
    void commissionRule_calculate_tiered_withinRange() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.TIERED,
                new BigDecimal("3.0"), "NGN", new BigDecimal("1000"), new BigDecimal("50000"));

        BigDecimal commission = rule.calculate(new BigDecimal("10000"));
        assertEquals(new BigDecimal("300.0"), commission);
    }

    @Test
    void commissionRule_calculate_tiered_outOfRange() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.TIERED,
                new BigDecimal("3.0"), "NGN", new BigDecimal("1000"), new BigDecimal("50000"));

        BigDecimal commission = rule.calculate(new BigDecimal("500"));
        assertEquals(BigDecimal.ZERO, commission);
    }

    // ─── Commission Entries ─────────────────────────────────────────

    @Test
    void calculateCommission_withRule() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.PERCENTAGE,
                new BigDecimal("2.5"), "NGN", null, null);
        rule.setId(1L);

        when(commissionRuleRepo.findActiveForStore(1L)).thenReturn(List.of(rule));
        when(commissionEntryRepo.save(any())).thenAnswer(i -> {
            CommissionEntry e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        CommissionEntry entry = service.calculateCommission(1L, 100L, new BigDecimal("10000"));
        assertEquals(new BigDecimal("250.00"), entry.getCommissionAmount());
        assertEquals(1L, entry.getRuleId());
    }

    @Test
    void calculateCommission_noRule_zeroCommission() {
        when(commissionRuleRepo.findActiveForStore(1L)).thenReturn(List.of());
        when(commissionEntryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        CommissionEntry entry = service.calculateCommission(1L, 100L, new BigDecimal("10000"));
        assertEquals(BigDecimal.ZERO, entry.getCommissionAmount());
        assertNull(entry.getRuleId());
    }

    // ─── Disputes ───────────────────────────────────────────────────

    @Test
    void createDispute_savesAndReturns() {
        when(disputeRepo.save(any())).thenAnswer(i -> {
            Dispute d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        Dispute dispute = service.createDispute(
                1L, 100L, null, 10L, DisputeType.ORDER_QUALITY,
                "Wrong item received", "I ordered size M but got XL",
                DisputePriority.NORMAL);

        assertNotNull(dispute);
        assertEquals(DisputeStatus.OPEN, dispute.getStatus());
        assertEquals("Wrong item received", dispute.getSubject());
    }

    @Test
    void getDispute_throwsWhenNotFound() {
        when(disputeRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getDispute(999L));
    }

    @Test
    void updateDisputeStatus_changesStatus() {
        Dispute dispute = new Dispute(1L, 100L, null, 10L, DisputeType.PAYMENT,
                "Refund request", "Need refund", DisputePriority.HIGH);
        dispute.setId(1L);

        when(disputeRepo.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Dispute updated = service.updateDisputeStatus(1L, DisputeStatus.RESOLVED,
                "Refund processed", 20L);

        assertEquals(DisputeStatus.RESOLVED, updated.getStatus());
        assertEquals("Refund processed", updated.getResolution());
        assertEquals(20L, updated.getAssignedToId());
    }

    @Test
    void addDisputeMessage_movesToInReview() {
        Dispute dispute = new Dispute(1L, 100L, null, 10L, DisputeType.PAYMENT,
                "Refund request", "Need refund", DisputePriority.HIGH);
        dispute.setId(1L);
        dispute.setStatus(DisputeStatus.OPEN);

        when(disputeRepo.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeMessageRepo.save(any())).thenAnswer(i -> {
            DisputeMessage m = i.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(disputeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        DisputeMessage msg = service.addDisputeMessage(1L, 10L, "Here are the details");

        assertNotNull(msg);
        assertEquals(DisputeStatus.IN_REVIEW, dispute.getStatus());
    }

    @Test
    void listDisputes_throwsOnBadStatus() {
        assertThrows(BadRequestException.class,
                () -> service.listDisputes(1L, "BOGUS"));
    }

    // ─── getAllConfig ─────────────────────────────────────────────

    @Test
    void getAllConfig_returnsMap() {
        PlatformConfig c1 = new PlatformConfig("k1", "v1", null);
        PlatformConfig c2 = new PlatformConfig("k2", "v2", null);
        when(configRepo.findAll()).thenReturn(List.of(c1, c2));

        Map<String, String> result = service.getAllConfig();
        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
    }

    // ─── Commission Rules ───────────────────────────────────────

    @Test
    void listCommissionRules_delegatesToRepo() {
        when(commissionRuleRepo.findActiveForStore(1L)).thenReturn(List.of());
        assertEquals(0, service.listCommissionRules(1L).size());
    }

    @Test
    void updateCommissionRule_updatesRateAndActive() {
        CommissionRule rule = new CommissionRule(1L, CommissionRuleType.PERCENTAGE,
                new BigDecimal("2.5"), "NGN", null, null);
        rule.setId(1L);
        when(commissionRuleRepo.findById(1L)).thenReturn(Optional.of(rule));
        when(commissionRuleRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        CommissionRule updated = service.updateCommissionRule(1L, new BigDecimal("5.0"), false);
        assertEquals(new BigDecimal("5.0"), updated.getRate());
        assertFalse(updated.isActive());
    }

    @Test
    void updateCommissionRule_throwsWhenNotFound() {
        when(commissionRuleRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCommissionRule(999L, null, true));
    }

    // ─── Commission Entries ─────────────────────────────────────

    @Test
    void listCommissions_delegatesToRepo() {
        when(commissionEntryRepo.findByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertEquals(0, service.listCommissions(1L).size());
    }

    @Test
    void getCommissionSummary_returnsTotals() {
        when(commissionEntryRepo.sumCommissionByStoreAndStatus(1L, CommissionStatus.PENDING))
                .thenReturn(new BigDecimal("500"));
        when(commissionEntryRepo.sumCommissionByStoreAndStatus(1L, CommissionStatus.PAID))
                .thenReturn(new BigDecimal("1000"));
        when(commissionEntryRepo.countByStoreId(1L)).thenReturn(5L);

        Map<String, Object> summary = service.getCommissionSummary(1L);
        assertEquals(new BigDecimal("500"), summary.get("pendingAmount"));
        assertEquals(new BigDecimal("1000"), summary.get("paidAmount"));
        assertEquals(5L, summary.get("totalEntries"));
    }

    // ─── Disputes ───────────────────────────────────────────────

    @Test
    void listDisputes_withValidStatus() {
        when(disputeRepo.findByStatus(DisputeStatus.OPEN)).thenReturn(List.of());
        assertEquals(0, service.listDisputes(1L, "OPEN").size());
    }

    @Test
    void listDisputes_withNullStatus() {
        when(disputeRepo.findByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertEquals(0, service.listDisputes(1L, null).size());
    }

    @Test
    void listDisputeMessages_delegatesToRepo() {
        when(disputeMessageRepo.findByDisputeIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        assertEquals(0, service.listDisputeMessages(1L).size());
    }

    // ─── Analytics ──────────────────────────────────────────────

    @Test
    void getStoreAnalytics_computesMetrics() {
        when(salesSnapshotRepo.sumRevenueByStoreAndDateRange(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(salesSnapshotRepo.sumOrdersByStoreAndDateRange(eq(1L), any(), any()))
                .thenReturn(10L);
        when(salesSnapshotRepo.findByStoreIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.getStoreAnalytics(1L, 30);
        assertEquals(new BigDecimal("50000"), result.get("totalRevenue"));
        assertEquals(10L, result.get("totalOrders"));
        assertEquals(new BigDecimal("5000.00"), result.get("averageOrderValue"));
    }

    @Test
    void getStoreAnalytics_zeroOrders() {
        when(salesSnapshotRepo.sumRevenueByStoreAndDateRange(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(salesSnapshotRepo.sumOrdersByStoreAndDateRange(eq(1L), any(), any()))
                .thenReturn(0L);
        when(salesSnapshotRepo.findByStoreIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.getStoreAnalytics(1L, 7);
        assertEquals(BigDecimal.ZERO, result.get("averageOrderValue"));
    }

    @Test
    void getPlatformAnalytics_countsDisputes() {
        when(disputeRepo.countByStatus(DisputeStatus.OPEN)).thenReturn(3L);
        when(disputeRepo.countByStatus(DisputeStatus.IN_REVIEW)).thenReturn(2L);
        when(disputeRepo.countByStatus(DisputeStatus.RESOLVED)).thenReturn(5L);
        when(disputeRepo.countByStatus(DisputeStatus.CLOSED)).thenReturn(1L);
        when(disputeRepo.countByStatus(DisputeStatus.ESCALATED)).thenReturn(0L);

        Map<String, Object> result = service.getPlatformAnalytics();
        Map<String, Object> disputes = (Map<String, Object>) result.get("disputes");
        assertEquals(3L, disputes.get("open"));
        assertEquals(2L, disputes.get("inReview"));
        assertEquals(11L, disputes.get("total"));
    }
}
