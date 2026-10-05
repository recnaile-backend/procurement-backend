package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {

    @Mock
    private ReturnRequestRepository repository;

    @Mock
    private IssuedReturnValidationClient issuedReturnValidationClient;

    @Test
    void createBuildsPendingReturnAndPersistsIt() {
        Instant now = Instant.parse("2026-09-30T05:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        IssuedReturnValidationService validationService = new IssuedReturnValidationService(issuedReturnValidationClient);
        when(issuedReturnValidationClient.validateIssuedLine("OUT-100", "OUT-100-L1"))
                .thenReturn(new IssuedReturnValidation("OUT-100", "OUT-100-L1", "ITEM-10",
                        new BigDecimal("5"), new BigDecimal("1"), "ASSET-1"));
        ReturnRequestService service = new ReturnRequestService(repository, validationService, clock);

        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "ASSET-1", "user-1", ReturnCondition.GOOD, "  returned  ");

        ArgumentCaptor<ReturnRequest> captor = ArgumentCaptor.forClass(ReturnRequest.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest result = service.create(request);

        assertThat(result.getReturnId()).isNotBlank();
        assertThat(result.getOutwardId()).isEqualTo("OUT-100");
        assertThat(result.getOutwardLineId()).isEqualTo("OUT-100-L1");
        assertThat(result.getItemId()).isEqualTo("ITEM-10");
        assertThat(result.getQuantity()).isEqualByComparingTo("2");
        assertThat(result.getAssetId()).isEqualTo("ASSET-1");
        assertThat(result.getReturnedBy()).isEqualTo("user-1");
        assertThat(result.getCondition()).isEqualTo(ReturnCondition.GOOD);
        assertThat(result.getRemarks()).isEqualTo("returned");
        assertThat(result.getStatus()).isEqualTo(ReturnStatus.PENDING_VERIFICATION);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        verify(repository).save(captor.getValue());
    }
}
