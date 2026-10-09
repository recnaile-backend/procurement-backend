package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.recnaile.outward.client.auth.AuthEmployeeClient;
import com.recnaile.outward.client.auth.EmployeeResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MasterDataValidationServiceTest {

    @Mock
    private MasterDataClient masterDataClient;

    @Mock
    private AuthEmployeeClient authEmployeeClient;

    @Mock
    private Authentication authentication;

    private MasterDataValidationService service;

    @BeforeEach
    void setUp() {
        service = new MasterDataValidationService(masterDataClient, authEmployeeClient);
    }

    @Test
    void validatesActiveItemDepartmentAndProject() {
        useMdAuthentication();
        stubActiveDepartment();
        when(masterDataClient.getProject("PROJ-1"))
                .thenReturn(new ProjectResponse("PROJ-1", "Project One", "TL-1", "ACTIVE", null, null, 0L));
        stubActiveItem();
        when(authEmployeeClient.getEmployees())
                .thenReturn(List.of(employee("EMP-1", "Employee", "One", true)));

        MasterDataValidationService.ValidationResult result =
                service.validate(request("ITEM-1", new BigDecimal("2"), "PROJ-1", "Engineering"));

        assertEquals(true, result.assetTracked());
    }

    @Test
    void rejectsUnknownItem() {
        stubActiveDepartment();
        when(masterDataClient.getItem("ITEM-404")).thenReturn(null);

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-404", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_ITEM_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInactiveItem() {
        stubActiveDepartment();
        when(masterDataClient.getItem("ITEM-1"))
                .thenReturn(new ItemResponse("ITEM-1", "Laptop", "NOS", "REUSABLE",
                        new BigDecimal("5"), true, "INACTIVE"));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_ITEM_INACTIVE", exception.getCode());
    }

    @Test
    void doesNotValidateStockFromMasterData() {
        useMdAuthentication();
        stubActiveDepartment();
        stubActiveItem();
        when(authEmployeeClient.getEmployees())
                .thenReturn(List.of(employee("EMP-1", "Employee", "One", true)));

        MasterDataValidationService.ValidationResult result =
                service.validate(request("ITEM-1", new BigDecimal("999"), null, "Engineering"));

        assertEquals(true, result.assetTracked());
    }

    @Test
    void rejectsInactiveDepartment() {
        lenient().when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "INACTIVE", null, null, 0L)));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_DEPARTMENT_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInactiveProject() {
        stubActiveDepartment();
        lenient().when(masterDataClient.getProject("PROJ-1"))
                .thenReturn(new ProjectResponse("PROJ-1", "Project One", "TL-1", "INACTIVE", null, null, 0L));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), "PROJ-1", "Engineering")));

        assertEquals("OUTWARD_PROJECT_INACTIVE", exception.getCode());
    }

    @Test
    void rejectsUnknownRecipientEmployee() {
        useMdAuthentication();
        stubActiveDepartment();
        stubActiveItem();
        when(authEmployeeClient.getEmployees()).thenReturn(List.of());

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_EMPLOYEE_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInactiveRecipientEmployee() {
        useMdAuthentication();
        stubActiveDepartment();
        stubActiveItem();
        EmployeeResponse employee = employee("EMP-1", "Employee", "One", false);
        when(authEmployeeClient.getEmployees()).thenReturn(List.of(employee));

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering")));

        assertEquals("OUTWARD_EMPLOYEE_INACTIVE", exception.getCode());
    }

    @Test
    void rejectsRecipientNameMismatch() {
        useMdAuthentication();
        stubActiveDepartment();
        stubActiveItem();
        EmployeeResponse employee = employee("EMP-1", "John", "Doe", true);
        when(authEmployeeClient.getEmployees()).thenReturn(List.of(employee));

        CreateOutwardRequest request =
                request("ITEM-1", new BigDecimal("1"), null, "Engineering");
        request.getRecipient().setEmployeeName("Wrong Name");

        MasterDataValidationException exception = assertThrows(
                MasterDataValidationException.class,
                () -> service.validate(request));

        assertEquals("OUTWARD_EMPLOYEE_NAME_MISMATCH", exception.getCode());
    }

    @Test
    void acceptsMatchingActiveRecipient() {
        useMdAuthentication();
        stubActiveDepartment();
        when(authEmployeeClient.getEmployees())
                .thenReturn(List.of(employee("EMP-1", "Employee", "One", true)));
        stubActiveItem();

        MasterDataValidationService.ValidationResult result =
                service.validate(request("ITEM-1", new BigDecimal("1"), null, "Engineering"));

        assertEquals(true, result.assetTracked());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void useMdAuthentication() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MD")))
                .when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void stubActiveDepartment() {
        when(masterDataClient.getDepartments()).thenReturn(List.of(
                new DepartmentResponse("dep-1", "Engineering", null, "ACTIVE", null, null, 0L)));
    }

    private void stubActiveItem() {
        when(masterDataClient.getItem("ITEM-1"))
                .thenReturn(new ItemResponse("ITEM-1", "Laptop", "NOS", "REUSABLE",
                        new BigDecimal("5"), true, "ACTIVE"));
    }

    private EmployeeResponse employee(String employeeId, String firstName,
                                      String lastName, boolean enabled) {
        EmployeeResponse employee = new EmployeeResponse();
        employee.setEmployeeId(employeeId);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEnabled(enabled);
        return employee;
    }

    private CreateOutwardRequest request(String itemId, BigDecimal quantity,
                                         String projectId, String department) {
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
