package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderCommandValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void approvedIndentCommandExposesOnlyAllowedPoFields() {
        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "", null, "", null);
        var violations = validator.validate(command);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("indentNumber", "purchaseType", "vendorName")
                .doesNotContain("indentStatus", "departmentName", "projectId", "items");
    }

    @Test
    void approvedIndentCommandAcceptsExpectedProductionRequestShape() {
        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "IN%2F26-27%2F013", PurchaseType.STANDARD, "ABC Motors",
                java.time.LocalDate.of(2026, 10, 20));

        assertThat(validator.validate(command)).isEmpty();
    }
}
