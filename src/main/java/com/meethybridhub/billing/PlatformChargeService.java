package com.meethybridhub.billing;

import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dormant platform-charge module (see docs/platform-charge-dormant-module.md).
 *
 * Charges a flat fee per transaction behind the scenes. It ships ASLEEP:
 * {@code platform-charge.enabled} defaults to {@code false}, in which case
 * {@link #charge} and {@link #sweep} are no-ops and nothing is persisted.
 * Wake it up with {@code PLATFORM_CHARGE_ENABLED=true} + a flat fee in the
 * environment; from then on inline {@code charge()} calls and the nightly
 * sweep start recording automatically.
 *
 * Idempotency: the {@code transaction_ref} column is unique, so a transaction
 * can never be charged twice regardless of retries or duplicate sweeps.
 */
@Service
public class PlatformChargeService {

    private static final Logger log = LoggerFactory.getLogger(PlatformChargeService.class);

    private final PlatformChargeRepository platformChargeRepository;
    private final List<ChargeableTransactionSource> transactionSources;
    private final AuditLogService auditLogService;

    @Value("${platform-charge.enabled:false}")
    private boolean enabled;

    @Value("${platform-charge.flat-fee:0}")
    private BigDecimal flatFee;

    @Value("${platform-charge.currency:NGN}")
    private String currency;

    public PlatformChargeService(PlatformChargeRepository platformChargeRepository,
                                 List<ChargeableTransactionSource> transactionSources,
                                 AuditLogService auditLogService) {
        this.platformChargeRepository = platformChargeRepository;
        this.transactionSources = transactionSources;
        this.auditLogService = auditLogService;
    }

    /** Whether the module is awake (the sleep switch). */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Record the flat fee on a single transaction.
     *
     * @return the persisted charge, or empty when the module is dormant or the
     *         transaction has already been charged
     */
    public Optional<PlatformCharge> charge(String transactionRef, BigDecimal amount) {
        if (!enabled) {
            log.info("Platform charge dormant (enabled=false); skipping transaction {}", transactionRef);
            return Optional.empty();
        }
        if (platformChargeRepository.existsByTransactionRef(transactionRef)) {
            log.debug("Transaction {} already charged; skipping", transactionRef);
            return Optional.empty();
        }

        PlatformCharge charge = new PlatformCharge(transactionRef, amount, flatFee, currency);
        final PlatformCharge saved;
        try {
            saved = platformChargeRepository.save(charge);
        } catch (DataIntegrityViolationException e) {
            // Lost the exists-then-save race: another request charged this ref
            // first. The unique transaction_ref is the real idempotency guard.
            log.info("Transaction {} was charged concurrently; skipping", transactionRef);
            return Optional.empty();
        }
        auditLogService.record(null, AuditEventType.PLATFORM_CHARGE_RECORDED,
                "Platform charge " + currency + " " + flatFee + " recorded on transaction " + transactionRef,
                null, null);
        log.info("Platform charge recorded: {} {} on transaction {}", currency, flatFee, transactionRef);
        return Optional.of(saved);
    }

    /**
     * Nightly sweep: find settled transactions from every registered
     * {@link ChargeableTransactionSource}, skip refs already charged, and
     * charge the rest. No-op while dormant or with no sources registered
     * (the state of the codebase today).
     *
     * CAVEAT: the whole sweep runs in ONE transaction. If a single charge
     * fails, the entire batch rolls back — and a failed audit insert inside
     * the shared transaction marks it rollback-only (see AuditLogService).
     * Acceptable for a nightly job whose batch is pre-filtered; revisit
     * per-transaction isolation if a batch ever fails in practice.
     */
    @Scheduled(cron = "${platform-charge.sweep-cron:0 0 3 * * *}")
    @Transactional
    public void sweep() {
        if (!enabled) {
            log.info("Platform charge dormant (enabled=false); sweep skipped");
            return;
        }
        if (transactionSources.isEmpty()) {
            log.info("No ChargeableTransactionSource implementations registered; platform charge sweep is a no-op");
            return;
        }

        Instant cutoff = Instant.now();
        for (ChargeableTransactionSource source : transactionSources) {
            List<ChargeableTransaction> transactions = source.findSettledTransactionsBefore(cutoff);
            if (transactions.isEmpty()) {
                continue;
            }

            Set<String> refs = transactions.stream()
                    .map(ChargeableTransaction::transactionRef)
                    .collect(Collectors.toSet());
            Set<String> alreadyCharged = platformChargeRepository.findByTransactionRefIn(refs).stream()
                    .map(PlatformCharge::getTransactionRef)
                    .collect(Collectors.toSet());

            for (ChargeableTransaction transaction : transactions) {
                if (alreadyCharged.contains(transaction.transactionRef())) {
                    continue;
                }
                // charge() re-checks existsByTransactionRef as its own guard —
                // deliberate, so it stays idempotent when the future Orders
                // module calls it inline. One extra query per ref is the cost.
                charge(transaction.transactionRef(), transaction.amount());
            }
        }
    }
}
