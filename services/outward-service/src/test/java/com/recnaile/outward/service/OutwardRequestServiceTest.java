package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.dto.CreateOutwardItem;
import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.RecipientRequest;
import com.recnaile.outward.repository.OutwardRequestRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutwardRequestServiceTest {

    @Mock
    private OutwardRequestRepository repository;

    @Mock
    private OutwardNumberService numberService;

    @InjectMocks
    private OutwardRequestService service;

    @Test
    void createsDraftUsingAuthenticatedUser() {
        Instant now = Instant.parse("2026-09-26T05:00:00Z");
        service = new OutwardRequestService(repository, numberService, Clock.fixed(now, ZoneOffset.UTC));
        when(numberService.nextNumber()).thenReturn("OUT%2F26-27%2F001");
        when(repository.save(any(OutwardRequest.class))).thenAnswer(invocation -> {
            OutwardRequest saved = invocation.getArgument(0);
            saved.setId("request-1");
            saved.setVersion(0L);
            return saved;
        });

        CreateOutwardItem item = new CreateOutwardItem();
        item.setItemId(" ITEM-001 ");
        item.setQuantity(new BigDecimal("2"));
        CreateOutwardRequest request = new CreateOutwardRequest();
        request.setItems(List.of(item));
        request.setProjectId(" PROJECT-1 ");
        request.setDepartmentName(" Engineering ");
        request.setPurpose(" Laptop issue ");
        RecipientRequest recipient = new RecipientRequest();
        recipient.setEmployeeId(" EMP-1 ");
        recipient.setEmployeeName(" Rajesh ");
        request.setRecipient(recipient);

        var response = service.create(request, "jwt-user");

        assertEquals("request-1", response.id());
        assertEquals("OUT%2F26-27%2F001", response.outwardNumber());
        assertEquals("jwt-user", response.requestedBy());
        assertEquals("DRAFT", response.status().name());
        assertEquals("ITEM-001", response.items().get(0).itemId());
        assertEquals("Engineering", response.departmentName());
    }
}
