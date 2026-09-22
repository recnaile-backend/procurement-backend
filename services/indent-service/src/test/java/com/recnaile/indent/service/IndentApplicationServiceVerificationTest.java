package com.recnaile.indent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.exception.InvalidIndentTransitionException;
import com.recnaile.indent.repository.IndentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndentApplicationServiceVerificationTest {

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
    private IndentNumberGenerator indentNumberGenerator;

    @Mock
    private OutboxEventService outboxEventService;

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
            outboxEventService
        );
    }

    @Test
    void verify_shouldMoveSubmittedIndentToPendingApprovalAndRecordVerifier() {

        when(approvalProperties.getThreshold())
                .thenReturn(new BigDecimal("500.00"));

        when(approvalProperties.getSlaDuration())
                .thenReturn(Duration.ofHours(24));

        Indent indent = new Indent();

        indent.setId("IND-1");
        indent.setIndentNumber("IND-1");
        indent.setStatus(IndentStatus.SUBMITTED);
        indent.setEstimatedTotalAmount(
                new BigDecimal("1250.00"));

        when(indentRepository.findByIndentNumber("IND-1"))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var response =
                service.verify(
                        "IND-1",
                        "procurement-user",
                        "Checked");

        assertThat(response.id())
                .isEqualTo("IND-1");

        assertThat(response.status())
                .isEqualTo(IndentStatus.PENDING_APPROVAL);

        assertThat(indent.getVerifierId())
                .isEqualTo("procurement-user");

        assertThat(indent.getVerificationRemarks())
                .isEqualTo("Checked");

        assertThat(indent.getVerifiedAt())
                .isNotNull();

        assertThat(indent.getApprovalLevel())
                .isEqualTo(ApprovalLevel.MD);

        assertThat(indent.getApprovalDueAt())
                .isNotNull();

        verify(indentRepository)
                .save(indent);
    }

    @Test
    void verify_shouldNotAllowAlreadyVerifiedIndent() {

        Indent indent = new Indent();

        indent.setId("IND-2");
        indent.setIndentNumber("IND-2");
        indent.setStatus(IndentStatus.VERIFIED);

        when(indentRepository.findByIndentNumber("IND-2"))
                .thenReturn(Optional.of(indent));

        assertThatThrownBy(() ->
                service.verify(
                        "IND-2",
                        "procurement-user",
                        null))
                .isInstanceOf(
                        InvalidIndentTransitionException.class);

        verify(indentRepository, never())
                .save(any());
    }

    @Test
    void autoVerify_shouldClassifyEachItemUsingMasterData() {

        Indent indent = new Indent();
        indent.setIndentNumber("IN%2F26-27%2F004");
        indent.setItems(java.util.List.of(
                new com.recnaile.indent.domain.model.IndentItem(
                        "SN-1", "Laptop", 10,
                        new BigDecimal("100.00"),
                        new BigDecimal("1000.00"), null),
                new com.recnaile.indent.domain.model.IndentItem(
                        "SN-2", "Mouse", 5,
                        new BigDecimal("20.00"),
                        new BigDecimal("100.00"), null),
                new com.recnaile.indent.domain.model.IndentItem(
                        "SN-3", "Nuts", 2,
                        new BigDecimal("30.00"),
                        new BigDecimal("60.00"), null)));

        when(indentRepository.findByIndentNumber("IN%2F26-27%2F004"))
                .thenReturn(Optional.of(indent));

        when(masterDataClient.getAllItems())
                .thenReturn(java.util.List.of(
                        new com.recnaile.indent.client.masterdata.ItemMasterResponse(
                                "ITEM-1", "Laptop", "Electronics", "PCS", "ASSET",
                                false, new BigDecimal("100.00"), null, null, null,
                                "ACTIVE", null, null, 1L),
                        new com.recnaile.indent.client.masterdata.ItemMasterResponse(
                                "ITEM-2", "Mouse", "Accessories", "PCS", "CONSUMABLE",
                                false, new BigDecimal("20.00"), null, null, null,
                                "ACTIVE", null, null, 1L)));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.autoVerifyInventory("IN%2F26-27%2F004");

        assertThat(response.items()).hasSize(3);
        assertThat(response.items().get(0).status()).isEqualTo("AVAILABLE");
        assertThat(response.items().get(1).status()).isEqualTo("AVAILABLE");
        assertThat(response.items().get(2).status()).isEqualTo("NOT_AVAILABLE");

        assertThat(indent.getItems().get(0).getVerificationStatus())
                .isEqualTo("AVAILABLE");
        assertThat(indent.getItems().get(1).getVerificationStatus())
                .isEqualTo("AVAILABLE");
        assertThat(indent.getItems().get(2).getVerificationStatus())
                .isEqualTo("NOT_AVAILABLE");

        verify(masterDataClient).getAllItems();
        verify(indentRepository).save(indent);
    }
}