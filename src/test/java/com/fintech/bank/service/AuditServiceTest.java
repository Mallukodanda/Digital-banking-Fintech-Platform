package com.fintech.bank.service;

import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.dto.AuditLogResponse;
import com.fintech.bank.entity.AuditAction;
import com.fintech.bank.entity.AuditLog;
import com.fintech.bank.entity.AuditResult;
import com.fintech.bank.exception.ResourceNotFoundException;
import com.fintech.bank.repository.AuditLogRepository;
import com.fintech.bank.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
@SuppressWarnings("null")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private UUID sampleActorId;
    private UUID sampleLogId;

    @BeforeEach
    void setUp() {
        sampleActorId = UUID.randomUUID();
        sampleLogId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Record Audit Action Tests")
    class RecordActionTests {

        @Test
        @DisplayName("Should record standard audit action with default SUCCESS result")
        void shouldRecordStandardAuditAction() {
            auditService.recordAction(
                    sampleActorId,
                    "ROLE_CUSTOMER",
                    AuditAction.TRANSFER_INITIATED,
                    "TRANSFER",
                    "TXN-123456",
                    "corr-999",
                    "192.168.1.1"
            );

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, times(1)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getActorId()).isEqualTo(sampleActorId);
            assertThat(saved.getAction()).isEqualTo(AuditAction.TRANSFER_INITIATED);
            assertThat(saved.getResourceType()).isEqualTo("TRANSFER");
            assertThat(saved.getResourceId()).isEqualTo("TXN-123456");
            assertThat(saved.getResult()).isEqualTo(AuditResult.SUCCESS);
            assertThat(saved.getCorrelationId()).isEqualTo("corr-999");
            assertThat(saved.getClientIp()).isEqualTo("192.168.1.1");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should record full enterprise audit action with metadata and failure status")
        void shouldRecordFullAuditActionWithMetadata() {
            String metadataJson = "{\"reason\":\"INVALID_CREDENTIALS\",\"attempts\":3}";

            auditService.recordAction(
                    sampleActorId,
                    "user@bank.com",
                    "ROLE_CUSTOMER",
                    AuditAction.AUTH_LOGIN_FAILED,
                    "USER",
                    sampleActorId.toString(),
                    AuditResult.FAILURE,
                    "corr-auth-401",
                    "203.0.113.195",
                    metadataJson
            );

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, times(1)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getActorUsername()).isEqualTo("user@bank.com");
            assertThat(saved.getAction()).isEqualTo(AuditAction.AUTH_LOGIN_FAILED);
            assertThat(saved.getResult()).isEqualTo(AuditResult.FAILURE);
            assertThat(saved.getMetadata()).isEqualTo(metadataJson);
            assertThat(saved.getCorrelationId()).isEqualTo("corr-auth-401");
        }
    }

    @Nested
    @DisplayName("Query & Filter Audit Logs Tests")
    class QueryAuditLogsTests {

        @Test
        @DisplayName("Should query audit logs with dynamic filter specification and pagination")
        void shouldQueryAuditLogsWithFilter() {
            AuditLogFilterRequest filter = new AuditLogFilterRequest(
                    sampleActorId,
                    "admin@bank.com",
                    "ROLE_ADMIN",
                    AuditAction.ACCOUNT_FROZEN,
                    "ACCOUNT",
                    "1000000001",
                    AuditResult.SUCCESS,
                    "corr-adm-1",
                    Instant.now().minusSeconds(3600),
                    Instant.now()
            );

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            AuditLog logEntry = new AuditLog(
                    sampleActorId, "admin@bank.com", "ROLE_ADMIN", AuditAction.ACCOUNT_FROZEN,
                    "ACCOUNT", "1000000001", AuditResult.SUCCESS, "corr-adm-1", "127.0.0.1", "{\"reason\":\"AML Hold\"}"
            );

            Page<AuditLog> pageResult = new PageImpl<>(List.of(logEntry), pageable, 1);
            when(auditLogRepository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(), eq(pageable))).thenReturn(pageResult);

            Page<AuditLogResponse> responsePage = auditService.getAuditLogs(filter, pageable);

            assertThat(responsePage.getTotalElements()).isEqualTo(1);
            assertThat(responsePage.getContent().get(0).action()).isEqualTo(AuditAction.ACCOUNT_FROZEN);
            assertThat(responsePage.getContent().get(0).actorUsername()).isEqualTo("admin@bank.com");
            assertThat(responsePage.getContent().get(0).result()).isEqualTo(AuditResult.SUCCESS);
        }

        @Test
        @DisplayName("Should retrieve individual audit log by Event ID")
        void shouldRetrieveAuditLogById() {
            AuditLog logEntry = new AuditLog(
                    sampleActorId, "user@bank.com", "ROLE_CUSTOMER", AuditAction.TRANSFER_COMPLETED,
                    "TRANSFER", "TXN-888", AuditResult.SUCCESS, "corr-888", "10.0.0.1", null
            );

            when(auditLogRepository.findById(sampleLogId)).thenReturn(Optional.of(logEntry));

            AuditLogResponse response = auditService.getAuditLogById(sampleLogId);

            assertThat(response).isNotNull();
            assertThat(response.action()).isEqualTo(AuditAction.TRANSFER_COMPLETED);
            assertThat(response.resourceId()).isEqualTo("TXN-888");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when audit log ID does not exist")
        void shouldThrowExceptionWhenLogNotFound() {
            when(auditLogRepository.findById(sampleLogId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditService.getAuditLogById(sampleLogId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("AuditLog entry not found");
        }
    }
}
