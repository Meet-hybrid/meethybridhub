package com.meethybridhub.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AuditLog} rows. Read-only in practice — audit entries
 * are append-only (see {@link AuditLogService}).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
