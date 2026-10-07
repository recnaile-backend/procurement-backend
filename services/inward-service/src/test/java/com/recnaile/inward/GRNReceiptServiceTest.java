package com.recnaile.inward;

import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.outbox.OutboxService;
import com.recnaile.inward.po.PurchaseOrderClient;
import com.recnaile.inward.po.PurchaseOrderLineSummary;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.repository.GRNRepository;
import com.recnaile.inward.service.GRNReceiptProgressService;
import com.recnaile.inward.service.GRNReceiptService;
import com.recnaile.inward.service.GRNStateMachine;
import com.recnaile.inward.service.IdempotencyService;
import com.recnaile.inward.service.PurchaseOrderEligibilityService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GRNReceiptServiceTest {

    private final GRNRepository repository = mock(GRNRepository.class);
    private final PurchaseOrderClient client = mock(PurchaseOrderClient.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private final GRNReceiptProgressService progressService = new GRNReceiptProgressService(repository);
    private final GRNReceiptService service = new GRNReceiptService(
            repository,
            new GRNStateMachine(),
            new PurchaseOrderEligibilityService(client),
            progressService,
            outboxService,
            clock,
            idempotencyService);

    @Test
    void partialReceiptConfirmsDraftAndPublishesOnlyAfterConfirmation() {
        PurchaseOrderSummary po = new PurchaseOrderSummary(
                "PO-1", "ISSUED", "3D",
                List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10"))));
        GRNDocument draft = new GRNDocument(
                "mongo-1", "GRN%2F26-27%2F001", "PO-1", "3D", "DN-1",
                Instant.parse("2026-10-06T00:00:00Z"), Instant.parse("2026-10-06T01:00:00Z"),
                "EMP001", GRNStatus.DRAFT,
                List.of(new GRNLine("ITEM-1", new BigDecimal("10"), new BigDecimal("6"), BigDecimal.ZERO, "PCS", null)));
        when(repository.findByGrnId("GRN%2F26-27%2F001")).thenReturn(java.util.Optional.of(draft));
        when(repository.findByPoId(eq("PO-1"), any())).thenReturn(List.of(draft));
        when(client.getPurchaseOrder("PO-1")).thenReturn(po);
        when(repository.save(any(GRNDocument.class))).thenAnswer(i -> i.getArgument(0));

        GRNResponse response = service.receive("GRN%2F26-27%2F001", null, "md-1");

        assertEquals(GRNStatus.PARTIALLY_RECEIVED, response.grnStatus());
        assertEquals(Instant.parse("2026-10-07T10:00:00Z"), response.receivedAt());
        verify(outboxService).enqueueGoodsReceived(response);
        verify(outboxService).enqueueGRNReceiptAuditEvent(response);
    }

    @Test
    void fullReceiptConfirmsDraftAsCompleted() {
        PurchaseOrderSummary po = new PurchaseOrderSummary(
                "PO-1", "ISSUED", "3D",
                List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10"))));
        GRNDocument draft = new GRNDocument(
                "mongo-1", "GRN%2F26-27%2F002", "PO-1", "3D", "DN-2",
                Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-07T01:00:00Z"),
                "EMP001", GRNStatus.DRAFT,
                List.of(new GRNLine("ITEM-1", new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO, "PCS", null)));
        when(repository.findByGrnId("GRN%2F26-27%2F002")).thenReturn(java.util.Optional.of(draft));
        when(repository.findByPoId(eq("PO-1"), any())).thenReturn(List.of(draft));
        when(client.getPurchaseOrder("PO-1")).thenReturn(po);
        when(repository.save(any(GRNDocument.class))).thenAnswer(i -> i.getArgument(0));

        GRNResponse response = service.receive("GRN%2F26-27%2F002", null, "md-1");

        assertEquals(GRNStatus.COMPLETED, response.grnStatus());
        verify(outboxService).enqueueGoodsReceived(response);
    }

    @Test
    void alreadyConfirmedGrnCannotBeReceivedAgain() {
        GRNDocument confirmed = new GRNDocument(
                "mongo-1", "GRN%2F26-27%2F003", "PO-1", "3D", "DN-3",
                Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-07T01:00:00Z"),
                "EMP001", GRNStatus.COMPLETED,
                List.of(new GRNLine("ITEM-1", new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO, "PCS", null)));
        when(repository.findByGrnId("GRN%2F26-27%2F003")).thenReturn(java.util.Optional.of(confirmed));

        assertThrows(com.recnaile.inward.exception.InvalidGRNStateTransitionException.class,
                () -> service.receive("GRN%2F26-27%2F003", null, "md-1"));
        verifyNoInteractions(client, outboxService);
    }
}
