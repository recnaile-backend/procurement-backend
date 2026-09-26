package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.domain.model.RecipientDetails;
import java.math.BigDecimal;
import com.recnaile.outward.repository.OutwardRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class OutwardApprovalServiceTest {

    @Mock OutwardRequestRepository repository;
    @Mock OutwardStateMachine stateMachine;
    @Mock ApprovalPolicy approvalPolicy;

    private OutwardApprovalService service;
    private final Instant now = Instant.parse("2026-09-26T05:00:00Z");

    @BeforeEach
    void setUp() {
        service = new OutwardApprovalService(repository, stateMachine, approvalPolicy,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void submitRequiresRequester() {
        OutwardRequest request = request("EMP-1", OutwardStatus.DRAFT);
        when(repository.findByOutwardNumber("OUT%2F26-27%2F001")).thenReturn(Optional.of(request));
        Authentication authentication = auth("EMP-2", "ROLE_EMPLOYEE");

        assertThrows(com.recnaile.outward.exception.OutwardException.class,
                () -> service.submit("OUT%2F26-27%2F001", authentication));
    }

    @Test
    void approveSetsApproverAndTimestamp() {
        OutwardRequest request = request("EMP-1", OutwardStatus.PENDING_APPROVAL);
        when(repository.findByOutwardNumber("OUT%2F26-27%2F001")).thenReturn(Optional.of(request));
        when(stateMachine.transition(OutwardStatus.PENDING_APPROVAL, OutwardStatus.APPROVED))
                .thenReturn(OutwardStatus.APPROVED);
        when(repository.save(any(OutwardRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication authentication = auth("TL-1", "ROLE_TL");

        var response = service.approve("OUT%2F26-27%2F001", authentication);

        assertEquals(OutwardStatus.APPROVED, response.status());
        assertEquals("TL-1", response.approvedBy());
        assertEquals(now, response.approvedAt());
    }

    @Test
    void rejectSetsRejectorAndReason() {
        OutwardRequest request = request("EMP-1", OutwardStatus.PENDING_APPROVAL);
        when(repository.findByOutwardNumber("OUT%2F26-27%2F001")).thenReturn(Optional.of(request));
        when(stateMachine.transition(OutwardStatus.PENDING_APPROVAL, OutwardStatus.REJECTED))
                .thenReturn(OutwardStatus.REJECTED);
        when(repository.save(any(OutwardRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication authentication = auth("MD-1", "ROLE_MD");

        var response = service.reject("OUT%2F26-27%2F001", "Insufficient justification", authentication);

        assertEquals(OutwardStatus.REJECTED, response.status());
        assertEquals("MD-1", response.rejectedBy());
        assertEquals("Insufficient justification", response.rejectionReason());
        assertEquals(now, response.rejectedAt());
    }

    private OutwardRequest request(String requester, OutwardStatus status) {
        OutwardRequest request = new OutwardRequest();
        request.setId("request-1");
        request.setOutwardNumber("OUT%2F26-27%2F001");
        request.setRequestedBy(requester);
        request.setStatus(status);
        request.setRequestedAt(now);
        request.setItems(List.of(new OutwardItem("ITEM-1", BigDecimal.ONE)));
        request.setDepartmentName("Engineering");
        request.setPurpose("Project requirement");
        request.setRecipient(new RecipientDetails(requester, "Test Employee"));
        return request;
    }

    private Authentication auth(String name, String role) {
        return new UsernamePasswordAuthenticationToken(name, "N/A",
                List.of(new SimpleGrantedAuthority(role)));
    }
}
