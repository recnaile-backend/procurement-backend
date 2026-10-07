package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.dto.PurchaseOrderResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderResponseTest {

    @Test
    void approvedIndentItemResponseUsesItemNameInApiContract() {
        assertThat(java.util.Arrays.stream(PurchaseOrderResponse.ApprovedIndentItemResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList())
                .containsExactly("itemName", "quantity", "estimatedUnitPrice", "estimatedLineAmount", "remarks");
    }

    @Test
    void approvedIndentResponseDoesNotExposeDepartmentId() {
        assertThat(java.util.Arrays.stream(PurchaseOrderResponse.ApprovedIndentResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList())
                .containsExactly(
                        "requesterId",
                        "departmentName",
                        "projectId",
                        "reason",
                        "items",
                        "estimatedTotalAmount",
                        "status",
                        "approvalLevel",
                        "verifierId",
                        "verifiedAt",
                        "verificationRemarks",
                        "approverId",
                        "approvedAt",
                        "approvalRemarks");
    }

    @Test
    void mapsLegacyPurchaseOrderWithNullCollectionsWithoutThrowing() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId("PO-001");
        po.setSourceIndentIds(null);
        po.setLines(null);

        PurchaseOrderResponse response = PurchaseOrderResponse.from(po);

        assertThat(response.poId()).isEqualTo("PO-001");
        assertThat(response.indentNumber()).isNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.indentDetails()).isNull();
    }

    @Test
    void mapsApprovedIndentSnapshotIntoProductionResponse() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId("PO-002");

        com.recnaile.purchaseorder.domain.model.ApprovedIndentSnapshot snapshot =
                new com.recnaile.purchaseorder.domain.model.ApprovedIndentSnapshot();
        snapshot.setRequesterId("EMP-001");
        snapshot.setDepartmentId("DEP-001");
        snapshot.setDepartmentName("Motor");
        snapshot.setProjectId("P-2");
        snapshot.setReason("For the Drone Motors");
        snapshot.setStatus("READY_FOR_PO");
        snapshot.setApprovalLevel("TL");
        snapshot.setVerifierId("PROC-001");
        snapshot.setApproverId("TL-001");

        po.setIndentDetails(snapshot);

        PurchaseOrderResponse response = PurchaseOrderResponse.from(po);

        assertThat(response.indentDetails()).isNotNull();
        assertThat(response.indentDetails().requesterId()).isEqualTo("EMP-001");
        assertThat(response.indentDetails().departmentName()).isEqualTo("Motor");
        assertThat(response.indentDetails().projectId()).isEqualTo("P-2");
        assertThat(response.indentDetails().verifierId()).isEqualTo("PROC-001");
        assertThat(response.indentDetails().approverId()).isEqualTo("TL-001");
    }
}
