package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }


    public void record(Long userId, AuditEventType eventType, String description,
                       String ipAddress, String userAgent) {
        try {
            auditLogRepository.save(new AuditLog(userId, eventType, description, ipAddress, userAgent));
        } catch (RuntimeException e) {
            log.error("Failed to record audit event {}: {}", eventType, e.getMessage());
        }
    }
}
