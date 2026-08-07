package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Writes to the immutable security audit trail ({@code audit_log}).
 *
 * Best-effort by design: the repository call is wrapped and failures are
 * logged, not rethrown. IMPORTANT nuance: when {@code record} runs inside an
 * OPEN transaction (e.g. REGISTER inside {@code UserService.register}), a
 * failed insert marks that transaction rollback-only, so the caller's commit
 * will still fail — the catch genuinely protects the standalone callers
 * (AuthController.login, AdminController), which each run in their own
 * transaction. This is accepted: audit integrity matters more than surviving a
 * broken audit table.
 *
 * The insert joins the CALLER's transaction (default REQUIRED): events recorded
 * while a service transaction is open (e.g. REGISTER inside
 * {@code UserService.register}) commit together with it, which keeps the
 * {@code user_id} foreign key valid. REQUIRES_NEW is deliberately NOT used — it
 * would violate that FK for events recorded before the referenced user/store
 * row is committed.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Append an audit entry. Never throws.
     *
     * @param userId      platform user who triggered the event, or null when
     *                    unauthenticated (e.g. a failed login)
     * @param eventType   the kind of event
     * @param description human-readable detail (e.g. what changed)
     * @param ipAddress   client IP if known in this layer, else null
     * @param userAgent   client User-Agent if known, else null
     */
    public void record(Long userId, AuditEventType eventType, String description,
                       String ipAddress, String userAgent) {
        try {
            auditLogRepository.save(new AuditLog(userId, eventType, description, ipAddress, userAgent));
        } catch (RuntimeException e) {
            log.error("Failed to record audit event {}: {}", eventType, e.getMessage());
        }
    }
}
