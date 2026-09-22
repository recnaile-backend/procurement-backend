package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.application.command.PurchaseOrderReasonRequest;
import com.recnaile.purchaseorder.controller.PurchaseOrderController;
import com.recnaile.purchaseorder.dto.PurchaseOrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderApiContractTest {

    @Test
    void shouldExposeDocumentedPurchaseOrderBasePath() {
        var mapping = PurchaseOrderController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(Arrays.asList(mapping.value())).containsExactly("/api/v1/purchase-orders");
    }

    @Test
    void commandEndpointsShouldAllowOptionalIdempotencyKey() {
        var commandMethods = Arrays.stream(PurchaseOrderController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .toList();

        assertThat(commandMethods).hasSize(5);

        assertThat(commandMethods)
                .allSatisfy(method -> assertThat(Arrays.stream(method.getParameterAnnotations())
                        .flatMap(Arrays::stream)
                        .filter(annotation -> annotation instanceof RequestHeader header
                                && "Idempotency-Key".equalsIgnoreCase(header.value()))
                        .map(annotation -> (RequestHeader) annotation)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Idempotency-Key header must be declared on " + method.getName())))
                        .extracting(RequestHeader::required)
                        .isEqualTo(false));
    }

    @Test
    void queryEndpointsShouldRemainGetOnly() {
        assertThat(Arrays.stream(PurchaseOrderController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .map(Method::getName)
                .toList())
                .containsExactlyInAnyOrder("getById", "getAll");
    }

    @Test
    void createCommandShouldExposeBusinessNamesInsteadOfIds() {
        assertThat(Arrays.stream(com.recnaile.purchaseorder.application.command.CreatePurchaseOrderRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList())
                .contains("vendorName", "departmentName", "purchaseType", "projectId", "items")
                .doesNotContain("poNumber", "indentStatus");

        assertThat(Arrays.stream(CreatePurchaseOrderFromApprovedIndentCommand.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList())
                .contains("indentNumber", "vendorName", "departmentName", "purchaseType", "projectId", "expectedDeliveryDate", "items")
                .doesNotContain("indentId", "vendorId", "departmentId", "indentStatus", "remarks");

        assertThat(Arrays.stream(PurchaseOrderReasonRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList())
                .containsExactly("reason");

        assertThat(Arrays.stream(PurchaseOrderResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList())
                .contains("indentNumber", "indentStatus", "vendorName", "departmentName", "items")
                .doesNotContain("sourceIndentIds", "vendorId", "departmentId", "remarks", "lines", "poNumber");
    }
}
