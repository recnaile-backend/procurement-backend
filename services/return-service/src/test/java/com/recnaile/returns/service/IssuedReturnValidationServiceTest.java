package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import com.recnaile.returns.model.enums.ReturnCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedReturnValidationServiceTest {

    @Mock
    private IssuedReturnValidationClient client;

    @Test
    void acceptsReturnWithinRemainingIssuedQuantity() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-1",
                        new BigDecimal("10"), new BigDecimal("4"), null));

        service.validate(request("OUT-1", "LINE-1", "ITEM-1", "5", null));
    }

    @Test
    void rejectsReturnWhenQuantityExceedsRemainingIssuedQuantity() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-1",
                        new BigDecimal("10"), new BigDecimal("4"), null));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "LINE-1", "ITEM-1", "7", null)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Return quantity exceeds the remaining issued quantity");
    }

    @Test
    void rejectsReturnWhenItemDoesNotMatchIssuedLine() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-2",
                        new BigDecimal("10"), BigDecimal.ZERO, null));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "LINE-1", "ITEM-1", "1", null)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Item ID does not match the issued line");
    }

    @Test
    void rejectsReturnWhenNoRemainingQuantityExists() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-1",
                        new BigDecimal("5"), new BigDecimal("5"), null));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "LINE-1", "ITEM-1", "1", null)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("No quantity remains available for return");
    }

    @Test
    void rejectsMismatchedAsset() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-1",
                        new BigDecimal("1"), BigDecimal.ZERO, "ASSET-1"));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "LINE-1", "ITEM-1", "1", "ASSET-2")))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Asset ID does not match the issued asset");
    }

    @Test
    void acceptsMatchingAsset() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedLine("OUT-1", "LINE-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "LINE-1", "ITEM-1",
                        new BigDecimal("1"), BigDecimal.ZERO, "ASSET-1"));

        service.validate(request("OUT-1", "LINE-1", "ITEM-1", "1", "ASSET-1"));
    }

    private CreateReturnRequest request(String outwardId, String lineId, String itemId, String quantity, String assetId) {
        return new CreateReturnRequest(outwardId, lineId, itemId, new BigDecimal(quantity),
                assetId, "user-1", ReturnCondition.GOOD, null);
    }
}
