package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedReturnValidationServiceTest {

    @Mock
    private IssuedReturnValidationClient client;

    @Test
    void acceptsReturnWithinRemainingIssuedQuantity() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-1", 10, 4));

        service.validate(request("OUT-1", "ITEM-1", 5));
    }

    @Test
    void rejectsReturnWhenQuantityExceedsRemainingIssuedQuantity() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-1", 10, 4));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "ITEM-1", 7)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Return quantity exceeds the remaining issued quantity");
    }

    @Test
    void rejectsReturnWhenItemDoesNotMatchIssuedItem() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-2", 10, 0));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "ITEM-1", 1)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Item ID does not match the issued item");
    }

    @Test
    void rejectsReturnWhenNoRemainingQuantityExists() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-1", 5, 5));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "ITEM-1", 1)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("No quantity remains available for return");
    }

    @Test
    void rejectsReusableReturnWithQuantityGreaterThanOne() {
        IssuedReturnValidationService service = new IssuedReturnValidationService(client);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-1", 5, 0, true));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "ITEM-1", 2)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Asset-tracked reusable returns must have quantity 1");
    }

    @Test
    void usesReturnServiceAcceptedQuantityInsteadOfOutwardZeroPlaceholder() {
        ReturnRequestRepository repository = mock(ReturnRequestRepository.class);
        IssuedReturnValidationService service = new IssuedReturnValidationService(client, repository);
        ReturnRequest prior = new ReturnRequest("OUT-1", "ITEM-1", 4, "user-1", ReturnCondition.GOOD, null, Instant.now());
        prior.setStatus(ReturnStatus.ACCEPTED_GOOD);
        when(client.validateIssuedItem("OUT-1", "ITEM-1"))
                .thenReturn(new IssuedReturnValidation("OUT-1", "ITEM-1", 10, 0, false));
        when(repository.findByOutwardIdAndItemIdAndStatusIn(
                "OUT-1", "ITEM-1", List.of(ReturnStatus.ACCEPTED_GOOD, ReturnStatus.ACCEPTED_DAMAGED)))
                .thenReturn(List.of(prior));

        assertThatThrownBy(() -> service.validate(request("OUT-1", "ITEM-1", 7)))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Return quantity exceeds the remaining issued quantity");
    }

    private CreateReturnRequest request(String outwardId, String itemId, int quantity) {
        return new CreateReturnRequest(outwardId, itemId, quantity,
                "user-1", ReturnCondition.GOOD, null);
    }
}
