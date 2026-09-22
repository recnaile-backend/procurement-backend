package com.recnaile.indent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.dto.request.ApproveIndentRequest;
import com.recnaile.indent.dto.request.RejectIndentRequest;
import com.recnaile.indent.repository.IndentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndentApprovalServiceTest {

    @Mock
    IndentRepository indentRepository;

    @Mock
    MasterDataClient masterDataClient;

    @Mock
    ApprovalProperties approvalProperties;

    @Mock
    IdempotencyService idempotencyService;

    @Mock
    IdempotencyProperties idempotencyProperties;

    @Mock
    IndentNumberGenerator indentNumberGenerator;

    @Mock
    OutboxEventService outboxEventService;

    private IndentApplicationService service;

    @BeforeEach
    void setUp() {

        service = new IndentApplicationService(
                indentRepository,
                indentNumberGenerator,
                masterDataClient,
                approvalProperties,
                idempotencyService,
                idempotencyProperties,
                new ObjectMapper(),
                outboxEventService);
    }

    @Test
    void approve_shouldMovePendingIndentToReadyForPo() {

        Indent indent =
                indent(
                        "IND-1",
                        IndentStatus.PENDING_APPROVAL);

        indent.setApprovalLevel(
                ApprovalLevel.TL);

        when(idempotencyService.requestHash(any()))
                .thenReturn("hash");

        when(idempotencyService.find(
                "tl-user",
                "APPROVE",
                "key"))
                .thenReturn(Optional.empty());

        when(indentRepository.findByIndentNumber("IND-1"))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var result =
                service.approve(
                        "IND-1",
                        "tl-user",
                        "TL",
                        new ApproveIndentRequest("Approved"),
                        "key");

        assertThat(result.status())
                .isEqualTo(IndentStatus.READY_FOR_PO);

        assertThat(indent.getApproverId())
                .isEqualTo("tl-user");

        assertThat(indent.getApprovedAt())
                .isNotNull();

        verify(indentRepository)
                .save(indent);
    }

    @Test
    void approve_shouldRejectWrongApprovalRole() {

        Indent indent =
                indent(
                        "IND-2",
                        IndentStatus.PENDING_APPROVAL);

        indent.setApprovalLevel(
                ApprovalLevel.MD);

        when(idempotencyService.requestHash(any()))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber("IND-2"))
                .thenReturn(Optional.of(indent));

        assertThatThrownBy(() ->
                service.approve(
                        "IND-2",
                        "tl-user",
                        "TL",
                        new ApproveIndentRequest("Approved"),
                        null))
                .isInstanceOf(RuntimeException.class);

        verify(indentRepository, never())
                .save(any());
    }

    @Test
    void approve_shouldRejectAlreadyReadyIndent() {

        Indent indent =
                indent(
                        "IND-3",
                        IndentStatus.READY_FOR_PO);

        indent.setApprovalLevel(
                ApprovalLevel.TL);

        when(idempotencyService.requestHash(any()))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber("IND-3"))
                .thenReturn(Optional.of(indent));

        assertThatThrownBy(() ->
                service.approve(
                        "IND-3",
                        "tl-user",
                        "TL",
                        null,
                        null))
                .isInstanceOf(RuntimeException.class);

        verify(indentRepository, never())
                .save(any());
    }

    @Test
    void reject_shouldRecordReasonAndActor() {

        Indent indent =
                indent(
                        "IND-4",
                        IndentStatus.PENDING_APPROVAL);

        indent.setApprovalLevel(
                ApprovalLevel.MD);

        when(idempotencyService.requestHash(any()))
                .thenReturn("hash");

        when(indentRepository.findByIndentNumber("IND-4"))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var result =
                service.reject(
                        "IND-4",
                        "md-user",
                        new RejectIndentRequest(
                                "Budget not approved"),
                        null);

        assertThat(result.status())
                .isEqualTo(IndentStatus.REJECTED);

        assertThat(indent.getRejectionReason())
                .isEqualTo("Budget not approved");

        assertThat(indent.getRejectionActorId())
                .isEqualTo("md-user");

        assertThat(indent.getRejectedAt())
                .isNotNull();
    }

    @Test
    void reject_shouldRequireReason() {

        assertThatThrownBy(() ->
                service.reject(
                        "IND-5",
                        "md-user",
                        new RejectIndentRequest(" "),
                        null))
                .isInstanceOf(RuntimeException.class);

        verify(indentRepository, never())
                .findByIndentNumber(any());

        verify(indentRepository, never())
                .save(any());
    }

    private Indent indent(
            String id,
            IndentStatus status) {

        Indent indent = new Indent();

        indent.setId(id);
        indent.setIndentNumber(id);
        indent.setStatus(status);
        indent.setEstimatedTotalAmount(
                new BigDecimal("1000.00"));

        return indent;
    }
}