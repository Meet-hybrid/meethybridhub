package com.meethybridhub.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the audit trail writer: entries are persisted with the actor,
 * event, description, IP and User-Agent; a repository failure must never
 * propagate to the caller (best-effort logging).
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void recordPersistsAuditEntryWithAllFields() {
        AuditLogService service = new AuditLogService(auditLogRepository);

        service.record(42L, AuditEventType.LOGIN_SUCCESS, "Login successful", "127.0.0.1", "curl/8.0");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(saved.getDescription()).isEqualTo("Login successful");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("curl/8.0");
    }

    @Test
    void recordAllowsNullActorForUnauthenticatedEvents() {
        AuditLogService service = new AuditLogService(auditLogRepository);

        service.record(null, AuditEventType.LOGIN_FAILED, "Failed login", "127.0.0.1", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
    }

    @Test
    void recordSwallowsRepositoryFailure() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(auditLogRepository).save(any());
        AuditLogService service = new AuditLogService(auditLogRepository);

        assertThatCode(() -> service.record(1L, AuditEventType.REGISTER, "User registered", null, null))
                .doesNotThrowAnyException();
    }
}
