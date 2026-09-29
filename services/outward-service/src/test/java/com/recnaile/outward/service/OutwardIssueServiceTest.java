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
import org.springframework.dao.OptimisticLockingFailureException;

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

        OutwardIssueTransactionResponse response = service.issue(request.getOutwardNumber(), "issue-key-1", authentication);

        assertEquals("OUT%2F26-27%2F001", response.outwardNumber());
        assertEquals("issuer-1", response.issuerId());
        assertEquals("approver-1", response.approverId());
        assertEquals(OutwardStatus.ISSUED, request.getStatus());
        verify(issueRepository).save(any(OutwardIssueTransaction.class));
        verify(requestRepository).save(request);
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalTransaction() {
        OutwardRequest request = approvedRequest();
        OutwardIssueTransaction existing = new OutwardIssueTransaction();
        existing.setId("issue-1");
        existing.setOutwardNumber(request.getOutwardNumber());
        existing.setIdempotencyKey("issue-key-replay");
        existing.setIssuedAt(fixed);
        existing.setItems(request.getItems());
        existing.setProjectId(request.getProjectId());
        existing.setRequestedBy(request.getRequestedBy());
        existing.setDepartmentName(request.getDepartmentName());
        existing.setRecipientEmployeeId(request.getRecipient().getEmployeeId());
        existing.setRecipientEmployeeName(request.getRecipient().getEmployeeName());
        existing.setIssuerId("issuer-1");
        existing.setApproverId(request.getApprovedBy());
        existing.setPurpose(request.getPurpose());
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(issueRepository.findByIdempotencyKey("issue-key-replay")).thenReturn(Optional.of(existing));

        OutwardIssueTransactionResponse response = service.issue(request.getOutwardNumber(), "issue-key-replay", authentication);

        assertEquals("issue-1", response.id());
        assertEquals("issue-key-replay", response.idempotencyKey());
        verifyNoInteractions(requestRepository);
        verify(issueRepository, never()).save(any());
    }

    @Test
    void rejectsReusingIdempotencyKeyForAnotherOutward() {
        OutwardIssueTransaction existing = new OutwardIssueTransaction();
        existing.setOutwardNumber("OUT%2F26-27%2F999");
        existing.setIdempotencyKey("same-key");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(issueRepository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        OutwardException ex = assertThrows(OutwardException.class,
                () -> service.issue("OUT%2F26-27%2F001", "same-key", authentication));

        assertEquals("OUTWARD_IDEMPOTENCY_KEY_REUSED", ex.getCode());
    }

    @Test
    void allowsIssueWithoutIdempotencyKey() {
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

        OutwardIssueTransactionResponse response = service.issue(request.getOutwardNumber(), null, authentication);

        assertEquals(request.getOutwardNumber(), response.outwardNumber());
        assertEquals(null, response.idempotencyKey());
        assertEquals(OutwardStatus.ISSUED, request.getStatus());
        verify(issueRepository, never()).findByIdempotencyKey(any());
        verify(issueRepository).save(any(OutwardIssueTransaction.class));
    }

    @Test
    void mapsConcurrentRequestUpdateToStableConflict() {
        OutwardRequest request = approvedRequest();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));
        when(issueRepository.findByIdempotencyKey("issue-key-concurrent")).thenReturn(Optional.empty());
        when(issueRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.empty());
        when(stateMachine.transition(OutwardStatus.APPROVED, OutwardStatus.ISSUED)).thenReturn(OutwardStatus.ISSUED);
        when(issueRepository.save(any(OutwardIssueTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(any(OutwardRequest.class)))
                .thenThrow(new OptimisticLockingFailureException("version changed"));

        OutwardException ex = assertThrows(OutwardException.class,
                () -> service.issue(request.getOutwardNumber(), "issue-key-concurrent", authentication));

        assertEquals("OUTWARD_CONCURRENT_MODIFICATION", ex.getCode());
    }

    @Test
    void issueRejectsNonApprovedRequest() {
        OutwardRequest request = approvedRequest();
        request.setStatus(OutwardStatus.PENDING_APPROVAL);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("issuer-1");
        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));

        OutwardException ex = assertThrows(OutwardException.class,
                () -> service.issue(request.getOutwardNumber(), "issue-key-2", authentication));

        assertEquals("OUTWARD_NOT_APPROVED", ex.getCode());
        verify(issueRepository, never()).save(any());
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
                () -> service.issue(request.getOutwardNumber(), "issue-key-3", authentication));

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
