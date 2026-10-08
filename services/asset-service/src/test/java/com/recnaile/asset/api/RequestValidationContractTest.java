package com.recnaile.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import com.recnaile.asset.api.dto.AssignAssetRequest;
import com.recnaile.asset.api.dto.CreateAssetRequest;
import com.recnaile.asset.api.dto.ReturnAssetRequest;
import com.recnaile.asset.model.AssetCondition;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationContractTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    @Test
    void createAssetRequiresCoreFields() {
        CreateAssetRequest request = new CreateAssetRequest("", "", "", null, null, "", null, null);

        Set<ConstraintViolation<CreateAssetRequest>> violations = validator.validate(request);

        assertEquals(5, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("itemId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("outwardNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("issuedBy")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("condition")));
    }

    @Test
    void assignmentRequiresEmployeeDepartmentIssuerAndOutward() {
        AssignAssetRequest request = new AssignAssetRequest("", "", "", "", null);

        Set<ConstraintViolation<AssignAssetRequest>> violations = validator.validate(request);

        assertEquals(4, violations.size());
    }

    @Test
    void returnRequiresReturnIdAndCondition() {
        ReturnAssetRequest request = new ReturnAssetRequest("", "", null);

        Set<ConstraintViolation<ReturnAssetRequest>> violations = validator.validate(request);

        assertEquals(3, violations.size());
    }

    @Test
    void validRequestsHaveNoViolations() {
        assertTrue(validator.validate(new CreateAssetRequest(
                "ITEM-1", "OUT-1", "Laptop", "Brand", "Model", "issuer-1", AssetCondition.GOOD, null)).isEmpty());
        assertTrue(validator.validate(new AssignAssetRequest(
                "EMP-1", "Engineering", "issuer-1", "OUT-1", null)).isEmpty());
        assertTrue(validator.validate(new ReturnAssetRequest("OUT-1", "RET-1", AssetCondition.GOOD)).isEmpty());
    }
}
