package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.domain.model.RecipientDetails;
import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import com.recnaile.outward.dto.OutwardIssueTransactionResponse;
import com.recnaile.outward.exception.OutwardException;
import com.recnaile.outward.repository.OutwardIssueTransactionRepository;
import com.recnaile.outward.repository.OutwardRequestRepository;
import java.math.BigDecimal;
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
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OutwardIssueServiceTest {

    @Mock OutwardRequestRepository requestRepository;
    @Mock OutwardIssueTransactionRepository issueRepository;
    @Mock OutwardStateMachine stateMachine;
    @Mock Authentication authentication;

    private OutwardIssueService service;
    private final Instant fixed = Instant.parse("2026-09-26T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new OutwardIssueService(requestRepository, issueRepository, stateMachine,
                Clock.fixed(fixed, ZoneOffset.UTC));
    }

    @Test
    void issueCreatesTransactionAndMarksRequestIssued() {
        OutwardRequest request = approvedRequest();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));
        when(issueRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.empty());
        when(stateMachine.transition(OutwardStatus.APPROVED, OutwardStatus.ISSUED)).thenReturn(OutwardStatus.ISSUED);
        when(issueRepository.save(any(OutwardIssueTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(any(OutwardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutwardIssueTransactionResponse response = service.issue(request.getOutwardNumber(), authentication);

        assertEquals("OUT%2F26-27%2F001", response.outwardNumber());
        assertEquals("issuer-1", response.issuerId());
        assertEquals("approver-1", response.approverId());
        assertEquals(OutwardStatus.ISSUED, request.getStatus());
        verify(issueRepository).save(any(OutwardIssueTransaction.class));
        verify(requestRepository).save(request);
    }

    @Test
    void issueRejectsNonApprovedRequest() {
        OutwardRequest request = approvedRequest();
        request.setStatus(OutwardStatus.PENDING_APPROVAL);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));

        OutwardException ex = assertThrows(OutwardException.class,
                () -> service.issue(request.getOutwardNumber(), authentication));

        assertEquals("OUTWARD_NOT_APPROVED", ex.getCode());
        verifyNoInteractions(issueRepository);
    }

    @Test
    void issueRejectsExistingTransaction() {
        OutwardRequest request = approvedRequest();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));
        when(issueRepository.findByOutwardNumber(request.getOutwardNumber()))
                .thenReturn(Optional.of(new OutwardIssueTransaction()));

        OutwardException ex = assertThrows(OutwardException.class,
                () -> service.issue(request.getOutwardNumber(), authentication));

        assertEquals("OUTWARD_ALREADY_ISSUED", ex.getCode());
        verify(issueRepository, never()).save(any());
    }

    private OutwardRequest approvedRequest() {
        OutwardRequest request = new OutwardRequest();
        request.setId("request-1");
        request.setOutwardNumber("OUT%2F26-27%2F001");
        request.setStatus(OutwardStatus.APPROVED);
        request.setRequestedAt(fixed);
        request.setRequestedBy("employee-1");
        request.setDepartmentName("Engineering");
        request.setPurpose("Project requirement");
        request.setProjectId("project-1");
        request.setRecipient(new RecipientDetails("employee-2", "Employee Two"));
        request.setApprovedBy("approver-1");
        request.setApprovedAt(fixed);
        request.setItems(List.of(new OutwardItem("item-1", BigDecimal.ONE)));
        request.setAssetTracked(true);
        return request;
    }
}
