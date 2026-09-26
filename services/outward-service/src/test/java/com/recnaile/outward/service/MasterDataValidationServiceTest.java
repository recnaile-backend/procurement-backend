package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.recnaile.outward.client.masterdata.DepartmentResponse;
import com.recnaile.outward.client.masterdata.ItemResponse;
import com.recnaile.outward.client.masterdata.MasterDataClient;
import com.recnaile.outward.client.masterdata.ProjectResponse;
import com.recnaile.outward.dto.CreateOutwardItem;
import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.RecipientRequest;
import com.recnaile.outward.exception.MasterDataValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterDataValidationServiceTest {

    @Mock
    private MasterDataClient masterDataClient;

    private MasterDataValidationService service;

    @BeforeEach
    void setUp() {
        service = new MasterDataValidationService(masterDataClient);
    }

    @Test
    void validatesActiveItemDepartmentAndProject() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
        when(masterDataClient.getProject("PROJ-1"))
                .thenReturn(new ProjectResponse("PROJ-1", "Project One", "TL-1", "ACTIVE", null, null, 0L));
        when(masterDataClient.getItem("ITEM-1"))
                .thenReturn(new ItemResponse("ITEM-1", "Laptop", "NOS", "REUSABLE", new BigDecimal("5"), true, "ACTIVE"));

        CreateOutwardRequest request = request("ITEM-1", new BigDecimal("2"), "PROJ-1", "Engineering");

        MasterDataValidationService.ValidationResult result = service.validate(request);

        assertEquals(true, result.assetTracked());
    }

    @Test
    void rejectsUnknownItem() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
        when(masterDataClient.getItem("ITEM-404")).thenReturn(null);

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-404", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_ITEM_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInactiveItem() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
        when(masterDataClient.getItem("ITEM-1"))
                .thenReturn(new ItemResponse("ITEM-1", "Laptop", "NOS", "REUSABLE", new BigDecimal("5"), true, "INACTIVE"));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_ITEM_INACTIVE", exception.getCode());
    }

    @Test
    void rejectsQuantityAboveAvailableStock() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
        when(masterDataClient.getItem("ITEM-1"))
                .thenReturn(new ItemResponse("ITEM-1", "Laptop", "NOS", "REUSABLE", new BigDecimal("2"), true, "ACTIVE"));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("3"), null, "Engineering")));

        assertEquals("OUTWARD_INSUFFICIENT_STOCK", exception.getCode());
    }

    @Test
    void rejectsInactiveDepartment() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "INACTIVE", null, null, 0L)));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_DEPARTMENT_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInactiveProject() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
        when(masterDataClient.getProject("PROJ-1"))
                .thenReturn(new ProjectResponse("PROJ-1", "Project One", "TL-1", "INACTIVE", null, null, 0L));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), "PROJ-1", "Engineering")));

        assertEquals("OUTWARD_PROJECT_INACTIVE", exception.getCode());
    }

    private CreateOutwardRequest request(String itemId, BigDecimal quantity, String projectId, String department) {
        CreateOutwardItem item = new CreateOutwardItem();
        item.setItemId(itemId);
        item.setQuantity(quantity);

        CreateOutwardRequest request = new CreateOutwardRequest();
        request.setItems(List.of(item));
        request.setProjectId(projectId);
        request.setDepartmentName(department);
        request.setPurpose("Issue for project work");
        RecipientRequest recipient = new RecipientRequest();
        recipient.setEmployeeId("EMP-1");
        recipient.setEmployeeName("Employee One");
        request.setRecipient(recipient);
        return request;
    }
}
