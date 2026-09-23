package com.recnaile.indent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.domain.model.IndentItem;
import com.recnaile.indent.dto.response.IndentDetailsResponse;
import com.recnaile.indent.dto.response.IndentListItemResponse;
import com.recnaile.indent.repository.IndentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndentQueryServiceTest {

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
    void list_shouldReturnPaginatedIndentSummaries() {

        Indent indent =
                indent(
                        "IND-1",
                        IndentStatus.PENDING_APPROVAL,
                        "250.00");

        PageRequest pageable =
                PageRequest.of(0, 20);

        when(indentRepository.findAllByFilters(
                IndentStatus.PENDING_APPROVAL,
                "DEPT-1",
                pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(indent),
                                pageable,
                                1));

        Page<IndentListItemResponse> result =
                service.list(
                        IndentStatus.PENDING_APPROVAL,
                        "DEPT-1",
                        pageable);

        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .singleElement()
                .satisfies(item -> {

                    assertThat(item.id())
                            .isEqualTo("IND-1");

                    assertThat(item.status())
                            .isEqualTo(
                                    IndentStatus.PENDING_APPROVAL);

                    assertThat(item.totalAmount())
                            .isEqualByComparingTo("250.00");
                });
    }

    @Test
    void getDetails_shouldDecodeUrlEncodedIndentNumber() {

        Indent indent =
                indent(
                        "IN%2F26-27%2F002",
                        IndentStatus.VERIFIED,
                        "330.00");

        when(indentRepository.findByIndentNumber(
                "IN%2F26-27%2F002"))
                .thenReturn(Optional.of(indent));

        IndentDetailsResponse result =
                service.getDetails(
                        "IN%2F26-27%2F002");

        assertThat(result.id())
                .isEqualTo("IN%2F26-27%2F002");
    }

    @Test
    void getDetails_shouldReturnIndentItemsAndTotal() {

        Indent indent =
                indent(
                        "IND-2",
                        IndentStatus.VERIFIED,
                        "330.00");

        indent.setItems(
                List.of(
                        new IndentItem(
                                "SERIAL-1",
                                "ITEM-1",
                                2,
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(220),
                                null)));

        when(indentRepository.findByIndentNumber("IND-2"))
                .thenReturn(Optional.of(indent));

        IndentDetailsResponse result =
                service.getDetails("IND-2");

        assertThat(result.id())
                .isEqualTo("IND-2");

        assertThat(result.status())
                .isEqualTo(IndentStatus.VERIFIED);

        assertThat(result.items())
                .hasSize(1);

        assertThat(result.items()
                .getFirst()
                .serialNumber())
                .isEqualTo("SERIAL-1");

        assertThat(result.totalAmount())
                .isEqualByComparingTo("330.00");
    }

    private static Indent indent(
            String id,
            IndentStatus status,
            String total) {

        Indent indent = new Indent();

        indent.setId(id);
        indent.setIndentNumber(id);
        indent.setStatus(status);

        indent.setEstimatedTotalAmount(
                new BigDecimal(total));

        return indent;
    }
}