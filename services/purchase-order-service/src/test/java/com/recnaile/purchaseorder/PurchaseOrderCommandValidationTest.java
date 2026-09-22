package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderCommandValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void approvedIndentCommandDoesNotExposeIndentStatus() {
        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "", "", null, "", null, null, List.of());
        var violations = validator.validate(command);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("indentNumber", "vendorName", "departmentName", "purchaseType", "items")
                .doesNotContain("indentStatus");
    }
}
