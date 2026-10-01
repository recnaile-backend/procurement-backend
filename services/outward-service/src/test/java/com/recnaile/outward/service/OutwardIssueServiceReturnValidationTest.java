package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.dto.IssuedReturnValidation;
import com.recnaile.outward.exception.OutwardException;
import com.recnaile.outward.outbox.AuditEventService;
import com.recnaile.outward.outbox.TransactionalOutboxService;
import com.recnaile.outward.repository.OutwardIssueTransactionRepository;
import com.recnaile.outward.repository.OutwardRequestRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutwardIssueServiceReturnValidationTest {

    private final OutwardRequestRepository requestRepository = mock(OutwardRequestRepository.class);
    private final OutwardIssueTransactionRepository issueRepository = mock(OutwardIssueTransactionRepository.class);
    private final OutwardStateMachine stateMachine = mock(OutwardStateMachine.class);
    private final TransactionalOutboxService outboxService = mock(TransactionalOutboxService.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);

    private final OutwardIssueService service = new OutwardIssueService(
            requestRepository, issueRepository, stateMachine, outboxService, auditEventService);

    @Test
    void validatesIssuedItem() {
        OutwardRequest request = new OutwardRequest();
        request.setOutwardNumber("OUT%2F26-27%2F004");
        request.setStatus(OutwardStatus.ISSUED);

        OutwardIssueTransaction issue = new OutwardIssueTransaction();
        issue.setOutwardNumber(request.getOutwardNumber());
        issue.setItems(List.of(new OutwardItem("Item-2", BigDecimal.valueOf(2))));

        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));
        when(issueRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(issue));

        IssuedReturnValidation result = service.validateReturn(request.getOutwardNumber(), "Item-2");

        assertThat(result.outwardId()).isEqualTo(request.getOutwardNumber());
        assertThat(result.itemId()).isEqualTo("Item-2");
        assertThat(result.issuedQuantity()).isEqualTo(2);
        assertThat(result.alreadyReturnedQuantity()).isZero();
    }

    @Test
    void rejectsOutwardThatIsNotIssued() {
        OutwardRequest request = new OutwardRequest();
        request.setOutwardNumber("OUT%2F26-27%2F004");
        request.setStatus(OutwardStatus.APPROVED);

        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.validateReturn(request.getOutwardNumber(), "Item-2"))
                .isInstanceOf(OutwardException.class)
                .hasMessage("Only an issued outward request can be returned");
    }

    @Test
    void rejectsItemNotPresentInIssueTransaction() {
        OutwardRequest request = new OutwardRequest();
        request.setOutwardNumber("OUT%2F26-27%2F004");
        request.setStatus(OutwardStatus.ISSUED);

        OutwardIssueTransaction issue = new OutwardIssueTransaction();
        issue.setOutwardNumber(request.getOutwardNumber());
        issue.setItems(List.of(new OutwardItem("Item-1", BigDecimal.ONE)));

        when(requestRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(request));
        when(issueRepository.findByOutwardNumber(request.getOutwardNumber())).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.validateReturn(request.getOutwardNumber(), "Item-2"))
                .isInstanceOf(OutwardException.class)
                .hasMessage("The requested item was not issued on this outward request");
    }
}
