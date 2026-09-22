package com.recnaile.indent.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.repository.IndentRepository;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRoutingTest {

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
    OutboxEventService outboxEventService;

    @Mock
    private IndentNumberGenerator indentNumberGenerator;

    private IndentApplicationService service;

    @BeforeEach
    void setUp() {

        when(approvalProperties.getThreshold())
                .thenReturn(BigDecimal.valueOf(500));

        when(approvalProperties.getSlaDuration())
                .thenReturn(Duration.ofHours(24));

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
    void verification_shouldRouteBelowThresholdToTl() {

        Indent indent = indent("IND-1", "100.00");

        when(indentRepository.findByIndentNumber("IND-1"))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.verify(
                "IND-1",
                "procurement-user",
                "Verified");

        assertThat(indent.getApprovalLevel())
                .isEqualTo(ApprovalLevel.TL);

        assertThat(indent.getStatus())
                .isEqualTo(IndentStatus.PENDING_APPROVAL);

        assertThat(indent.getApprovalDueAt())
                .isNotNull();
    }

    @Test
    void verification_shouldRouteAtThresholdToMd() {

        Indent indent = indent("IND-2", "500.00");

        when(indentRepository.findByIndentNumber("IND-2"))
                .thenReturn(Optional.of(indent));

        when(indentRepository.save(any(Indent.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.verify(
                "IND-2",
                "procurement-user",
                "Verified");

        assertThat(indent.getApprovalLevel())
                .isEqualTo(ApprovalLevel.MD);
    }

    private Indent indent(String id, String amount) {

        Indent indent = new Indent();

        indent.setId(id);
        indent.setIndentNumber(id);

        indent.setStatus(
                IndentStatus.SUBMITTED);

        indent.setEstimatedTotalAmount(
                new BigDecimal(amount));

        return indent;
    }
}