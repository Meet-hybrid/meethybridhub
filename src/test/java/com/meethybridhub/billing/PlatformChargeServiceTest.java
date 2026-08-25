package com.meethybridhub.billing;

import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the dormant platform-charge module:
 *   - while dormant (enabled=false): charge() and sweep() are no-ops
 *   - when enabled: the flat fee is persisted and audited
 *   - idempotent: a transaction can never be charged twice
 *   - the sweep charges only refs that aren't already charged
 */
@ExtendWith(MockitoExtension.class)
class PlatformChargeServiceTest {

    @Mock
    private PlatformChargeRepository platformChargeRepository;

    @Mock
    private ChargeableTransactionSource transactionSource;

    @Mock
    private AuditLogService auditLogService;

    private PlatformChargeService service;

    @BeforeEach
    void setUp() {
        service = new PlatformChargeService(platformChargeRepository,
                List.of(transactionSource), auditLogService);
    }

    @Test
    void chargeIsNoOpWhileDormant() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThat(service.charge("TXN-1", new BigDecimal("5000.00"))).isEmpty();
        verify(platformChargeRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void chargeRecordsFlatFeeWhenEnabled() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "flatFee", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(service, "currency", "NGN");

        when(platformChargeRepository.existsByTransactionRef("TXN-1")).thenReturn(false);
        when(platformChargeRepository.save(any(PlatformCharge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<PlatformCharge> result = service.charge("TXN-1", new BigDecimal("5000.00"));

        assertThat(result).isPresent();
        PlatformCharge charge = result.orElseThrow();
        assertThat(charge.getTransactionRef()).isEqualTo("TXN-1");
        assertThat(charge.getTransactionAmount()).isEqualByComparingTo("5000.00");
        assertThat(charge.getChargeAmount()).isEqualByComparingTo("50.00");
        assertThat(charge.getCurrency()).isEqualTo("NGN");
        assertThat(charge.getStatus()).isEqualTo(PlatformCharge.Status.PENDING);

        verify(auditLogService).record(isNull(), eq(AuditEventType.PLATFORM_CHARGE_RECORDED),
                anyString(), isNull(), isNull());
    }

    @Test
    void chargeIsIdempotent() {
        ReflectionTestUtils.setField(service, "enabled", true);
        when(platformChargeRepository.existsByTransactionRef("TXN-1")).thenReturn(true);

        assertThat(service.charge("TXN-1", new BigDecimal("5000.00"))).isEmpty();
        verify(platformChargeRepository, never()).save(any());
    }

    @Test
    void sweepIsNoOpWhileDormant() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.sweep();

        verifyNoInteractions(transactionSource);
        verify(platformChargeRepository, never()).save(any());
    }

    @Test
    void sweepIsNoOpWithNoSourcesRegistered() {
        // The exact state the module ships in: no ChargeableTransactionSource
        // implementations exist until the Orders/Payments phase lands.
        PlatformChargeService noSources = new PlatformChargeService(
                platformChargeRepository, List.of(), auditLogService);
        ReflectionTestUtils.setField(noSources, "enabled", true);

        noSources.sweep();

        verify(platformChargeRepository, never()).save(any());
    }

    @Test
    void sweepSkipsWhenSourceReportsNoTransactions() {
        ReflectionTestUtils.setField(service, "enabled", true);
        when(transactionSource.findSettledTransactionsBefore(any(Instant.class)))
                .thenReturn(List.of());

        service.sweep();

        verify(platformChargeRepository, never()).save(any());
    }

    @Test
    void sweepChargesOnlyUnchargedTransactions() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "flatFee", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(service, "currency", "NGN");

        when(transactionSource.findSettledTransactionsBefore(any(Instant.class)))
                .thenReturn(List.of(
                        new ChargeableTransaction("TXN-1", new BigDecimal("1000.00")),
                        new ChargeableTransaction("TXN-2", new BigDecimal("2000.00"))));
        // TXN-1 was already charged in a previous sweep; TXN-2 is new.
        when(platformChargeRepository.findByTransactionRefIn(anyCollection()))
                .thenReturn(List.of(platformCharge("TXN-1")));
        when(platformChargeRepository.existsByTransactionRef("TXN-2")).thenReturn(false);
        when(platformChargeRepository.save(any(PlatformCharge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sweep();

        verify(platformChargeRepository).save(argThat(c -> "TXN-2".equals(c.getTransactionRef())));
        verify(platformChargeRepository, never())
                .save(argThat(c -> "TXN-1".equals(c.getTransactionRef())));
    }

    @Test
    void isEnabledReflectsConfig() {
        ReflectionTestUtils.setField(service, "enabled", true);
        assertThat(service.isEnabled()).isTrue();

        ReflectionTestUtils.setField(service, "enabled", false);
        assertThat(service.isEnabled()).isFalse();
    }

    private PlatformCharge platformCharge(String transactionRef) {
        return new PlatformCharge(transactionRef, new BigDecimal("1000.00"),
                new BigDecimal("50.00"), "NGN");
    }
}
