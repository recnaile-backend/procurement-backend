package com.recnaile.indent.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.IdempotencyRecord;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.dto.request.ApproveIndentRequest;
import com.recnaile.indent.dto.response.IndentSummaryResponse;
import com.recnaile.indent.dto.request.RejectIndentRequest;
import com.recnaile.indent.exception.ConcurrentIndentModificationException;
import com.recnaile.indent.exception.IdempotencyKeyConflictException;
import com.recnaile.indent.repository.IndentRepository;
import com.recnaile.indent.service.OutboxEventService;

@ExtendWith(MockitoExtension.class)
class IndentApplicationServiceConcurrencyTest {

    @Mock
    private IndentRepository indentRepository;

    @Mock
    private MasterDataClient masterDataClient;

    @Mock
    private ApprovalProperties approvalProperties;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private IdempotencyProperties idempotencyProperties;

    @Mock
    private OutboxEventService outboxEventService;

    private ObjectMapper objectMapper;

    @Mock
    private IndentNumberGenerator indentNumberGenerator;

    private IndentApplicationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        service = new IndentApplicationService(
                indentRepository,
                indentNumberGenerator,
                masterDataClient,
                approvalProperties,
                idempotencyService,
                idempotencyProperties,
                objectMapper,
                outboxEventService);
    }

    @Test
    void approve_shouldReturnStoredResponse_whenIdempotencyKeyAlreadyProcessed() {

        String indentId = "IND-001";
        String approverId = "TL-001";
        String idempotencyKey = "approve-key-001";

        ApproveIndentRequest request =
                new ApproveIndentRequest("Approved");

        IndentSummaryResponse storedResponse =
                new IndentSummaryResponse(
                        indentId,
                        "IN%2F26-27%2F001",
                        approverId,
                        "DEPT-1",
                        "PROJECT-1",
                        "Test",
                        null,
                        java.time.Instant.now(),
                        IndentStatus.READY_FOR_PO,
                        BigDecimal.valueOf(1000));

        IdempotencyRecord record =
                IdempotencyRecord.builder()
                        .key(idempotencyKey)
                        .actorId(approverId)
                        .operation("APPROVE")
                        .resourceId(indentId)
                        .requestHash("same-hash")
                        .responseStatus(200)
                        .responseBody(toJson(storedResponse))
                        .build();

        when(idempotencyService.requestHash(request))
                .thenReturn("same-hash");

        when(idempotencyService.find(
                approverId,
                "APPROVE",
                idempotencyKey))
                .thenReturn(Optional.of(record));

        IndentSummaryResponse result =
                service.approve(
                        indentId,
                        approverId,
                        "TL",
                        request,
                        idempotencyKey);

        assertThat(result.status())
                .isEqualTo(IndentStatus.READY_FOR_PO);

        assertThat(result.id())
                .isEqualTo(indentId);

        verify(indentRepository, never())
                .findById(any());

        verify(indentRepository, never())
                .save(any());
    }

    @Test
    void approve_shouldReject_whenIdempotencyKeyUsedWithDifferentRequest() {

        String indentId = "IND-001";
        String approverId = "TL-001";
        String idempotencyKey = "approve-key-001";

        ApproveIndentRequest request =
                new ApproveIndentRequest("Different remarks");

        IdempotencyRecord record =
                IdempotencyRecord.builder()
                        .key(idempotencyKey)
                        .actorId(approverId)
                        .operation("APPROVE")
                        .resourceId(indentId)
                        .requestHash("original-hash")
                        .responseStatus(200)
                        .responseBody("{}")
                        .build();

        when(idempotencyService.requestHash(request))
                .thenReturn("different-hash");

        when(idempotencyService.find(
                approverId,
                "APPROVE",
                idempotencyKey))
                .thenReturn(Optional.of(record));

        doThrow(new IdempotencyKeyConflictException(
                "Idempotency-Key has already been used with a different request"))
                .when(idempotencyService)
                .assertSameRequest(
                        eq(record),
                        eq("different-hash"));

        assertThatThrownBy(() ->
                service.approve(
                        indentId,
                        approverId,
                        "TL",
                        request,
                        idempotencyKey))
                .isInstanceOf(IdempotencyKeyConflictException.class);

        verify(indentRepository, never())
                .findById(any());

        verify(indentRepository, never())
                .save(any());
    }

    @Test
    void approve_shouldThrowConcurrentModificationException_whenVersionIsStale() {

        String indentId = "IND-001";
        String approverId = "TL-001";

        ApproveIndentRequest request =
                new ApproveIndentRequest("Approved");

        Indent indent =
                pendingApprovalIndent(indentId);

        when(idempotencyService.requestHash(request))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber(indentId))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "Version conflict"));

        assertThatThrownBy(() ->
                service.approve(
                        indentId,
                        approverId,
                        "TL",
                        request,
                        null))
                .isInstanceOf(
                        ConcurrentIndentModificationException.class);

        verify(indentRepository)
                .save(any(Indent.class));

        verify(idempotencyService, never())
                .save(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(200),
                        any());
    }

    @Test
    void reject_shouldThrowConcurrentModificationException_whenVersionIsStale() {

        String indentId = "IND-001";
        String actorId = "TL-001";

        RejectIndentRequest request =
                new RejectIndentRequest("Budget rejected");

        Indent indent =
                pendingApprovalIndent(indentId);

        when(idempotencyService.requestHash(request))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber(indentId))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "Version conflict"));

        assertThatThrownBy(() ->
                service.reject(
                        indentId,
                        actorId,
                        request,
                        null))
                .isInstanceOf(
                        ConcurrentIndentModificationException.class);

        verify(indentRepository)
                .save(any(Indent.class));

        verify(idempotencyService, never())
                .save(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(200),
                        any());
    }

    @Test
    void approve_shouldMoveIndentToReadyForPo() {

        String indentId = "IND-001";
        String approverId = "TL-001";

        ApproveIndentRequest request =
                new ApproveIndentRequest("Approved");

        Indent indent =
                pendingApprovalIndent(indentId);

        when(idempotencyService.requestHash(request))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber(indentId))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        IndentSummaryResponse result =
                service.approve(
                        indentId,
                        approverId,
                        "TL",
                        request,
                        null);

        assertThat(result.status())
                .isEqualTo(IndentStatus.READY_FOR_PO);

        assertThat(indent.getApproverId())
                .isEqualTo(approverId);

        assertThat(indent.getStatus())
                .isEqualTo(IndentStatus.READY_FOR_PO);
    }

    @Test
    void reject_shouldMoveIndentToRejected() {

        String indentId = "IND-001";
        String actorId = "TL-001";

        RejectIndentRequest request =
                new RejectIndentRequest("Not required");

        Indent indent =
                pendingApprovalIndent(indentId);

        when(idempotencyService.requestHash(request))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber(indentId))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        IndentSummaryResponse result =
                service.reject(
                        indentId,
                        actorId,
                        request,
                        null);

        assertThat(result.status())
                .isEqualTo(IndentStatus.REJECTED);

        assertThat(indent.getRejectionActorId())
                .isEqualTo(actorId);

        assertThat(indent.getRejectionReason())
                .isEqualTo("Not required");
    }

    private Indent pendingApprovalIndent(String id) {

        Indent indent = new Indent();

        indent.setId(id);
        indent.setIndentNumber(id);

        indent.setStatus(
                IndentStatus.PENDING_APPROVAL);

        indent.setApprovalLevel(
                ApprovalLevel.TL);

        indent.setEstimatedTotalAmount(
                BigDecimal.valueOf(1000));

        indent.setVersion(1L);

        return indent;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to serialize test response",
                    ex);
        }
    }
}