package com.recnaile.inward;

import com.recnaile.inward.dto.CreateGRNLineRequest;
import com.recnaile.inward.dto.CreateGRNRequest;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.po.PurchaseOrderClient;
import com.recnaile.inward.po.PurchaseOrderLineSummary;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.repository.GRNRepository;
import com.recnaile.inward.service.GRNCreationService;
import com.recnaile.inward.service.GRNNumberGenerator;
import com.recnaile.inward.service.GRNReceiptProgressService;
import com.recnaile.inward.service.PurchaseOrderEligibilityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GRNCreationServiceTest {

    private final GRNRepository repository = mock(GRNRepository.class);
    private final PurchaseOrderClient client = mock(PurchaseOrderClient.class);
    private final GRNNumberGenerator grnNumberGenerator = mock(GRNNumberGenerator.class);
    private final GRNReceiptProgressService receiptProgressService = new GRNReceiptProgressService(repository);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-23T10:00:00Z"),
            ZoneOffset.UTC
    );
    private final GRNCreationService service = new GRNCreationService(
            repository,
            new PurchaseOrderEligibilityService(client),
            clock,
            grnNumberGenerator,
            receiptProgressService
    );

    @Test
    void createsDraftGRNFromIssuedPurchaseOrder() {
        PurchaseOrderSummary po = new PurchaseOrderSummary(
                "PO-1",
                "ISSUED",
                "3D",
                List.of(new PurchaseOrderLineSummary(
                        "PO-LINE-1",
                        "ITEM-1",
                        new BigDecimal("10")
                ))
        );

        when(client.getPurchaseOrder("PO-1")).thenReturn(po);
        when(grnNumberGenerator.next()).thenReturn("GRN/26-27/001");
        when(repository.save(any(GRNDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1",
                "DN-100",
                Instant.parse("2026-09-22T00:00:00Z"),
                "user-1",
                List.of(new CreateGRNLineRequest(
                        "ITEM-1",
                        "EA",
                        new BigDecimal("6"),
                        BigDecimal.ZERO,
                        null
                ))
        );

        GRNResponse response = service.create(request);

        assertNull(response.id());
        assertEquals("GRN/26-27/001", response.grnId());
        assertEquals("PO-1", response.poId());
        assertEquals("3D", response.vendorName());
        assertEquals("DN-100", response.deliveryNoteNumber());
        assertEquals("user-1", response.receivedBy());
        assertEquals("DRAFT", response.grnStatus().name());
        assertEquals(Instant.parse("2026-09-23T10:00:00Z"), response.receivedAt());
        assertEquals("ITEM-1", response.items().get(0).itemId());
        assertEquals(new BigDecimal("10"), response.items().get(0).orderedQuantity());
        assertEquals(new BigDecimal("6"), response.items().get(0).acceptedQuantity());
        verify(client).getPurchaseOrder("PO-1");
        verify(repository).save(any(GRNDocument.class));
    }

    @Test
    void rejectsLineThatDoesNotBelongToPurchaseOrder() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary(
                        "PO-1",
                        "ISSUED",
                        "3D",
                        List.of(new PurchaseOrderLineSummary(
                                "PO-LINE-1",
                                "ITEM-1",
                                new BigDecimal("10")
                        ))
                )
        );

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1",
                "DN-100",
                Instant.parse("2026-09-22T00:00:00Z"),
                "user-1",
                List.of(new CreateGRNLineRequest(
                        "OTHER-ITEM",
                        "EA",
                        new BigDecimal("1"),
                        BigDecimal.ZERO,
                        null
                ))
        );

        assertThrows(com.recnaile.inward.exception.ItemNotInPurchaseOrderException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsPurchaseOrderWithDuplicateItemIds() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary(
                        "PO-1",
                        "ISSUED",
                        "3D",
                        List.of(
                                new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10")),
                                new PurchaseOrderLineSummary("PO-LINE-2", "ITEM-1", new BigDecimal("5"))
                        )
                )
        );

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1",
                "DN-100",
                Instant.parse("2026-09-22T00:00:00Z"),
                "user-1",
                List.of(new CreateGRNLineRequest(
                        "ITEM-1",
                        "EA",
                        new BigDecimal("1"),
                        BigDecimal.ZERO,
                        null
                ))
        );

        com.recnaile.inward.exception.DuplicatePurchaseOrderItemException exception = assertThrows(
                com.recnaile.inward.exception.DuplicatePurchaseOrderItemException.class,
                () -> service.create(request)
        );

        assertTrue(exception.getMessage().contains("duplicate itemId"));
        verify(repository, never()).save(any());
    }

    @Test
    void requiresRejectionReasonForRejectedQuantity() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary(
                        "PO-1",
                        "ISSUED",
                        "3D",
                        List.of(new PurchaseOrderLineSummary(
                                "PO-LINE-1",
                                "ITEM-1",
                                new BigDecimal("10")
                        ))
                )
        );

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1",
                "DN-100",
                Instant.parse("2026-09-22T00:00:00Z"),
                "user-1",
                List.of(new CreateGRNLineRequest(
                        "ITEM-1",
                        "EA",
                        BigDecimal.ZERO,
                        new BigDecimal("1"),
                        null
                ))
        );

        assertThrows(com.recnaile.inward.exception.RejectionReasonRequiredException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }
    @Test
    void rejectsReceiptWhenCurrentLineExceedsOrderedQuantity() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary("PO-1", "ISSUED", "3D",
                        List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10")))));

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1", "DN-OVER", Instant.parse("2026-09-22T00:00:00Z"), "user-1",
                List.of(new CreateGRNLineRequest("ITEM-1", "EA", new BigDecimal("9"), new BigDecimal("2"), "Damaged")));

        assertThrows(com.recnaile.inward.exception.ReceivedQuantityExceedsOrderedException.class,
                () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsReceiptWhenCumulativeQuantityExceedsOrderedQuantity() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary("PO-1", "ISSUED", "3D",
                        List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10")))));

        GRNDocument previous = new GRNDocument(null, "GRN/26-27/001", "PO-1", "3D", "DN-OLD",
                Instant.parse("2026-09-21T00:00:00Z"), Instant.parse("2026-09-21T01:00:00Z"),
                "user-1", com.recnaile.inward.model.GRNStatus.DRAFT,
                List.of(new com.recnaile.inward.model.GRNLine("ITEM-1", new BigDecimal("10"),
                        new BigDecimal("7"), new BigDecimal("1"), "EA", "Damaged")));

        when(repository.findByPoId(eq("PO-1"), any())).thenReturn(List.of(previous));

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1", "DN-NEW", Instant.parse("2026-09-22T00:00:00Z"), "user-1",
                List.of(new CreateGRNLineRequest("ITEM-1", "EA", new BigDecimal("2"), new BigDecimal("1"), "Damaged")));

        assertThrows(com.recnaile.inward.exception.ReceivedQuantityExceedsOrderedException.class,
                () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void allowsPartialReceiptAcrossMultipleGRNs() {
        when(client.getPurchaseOrder("PO-1")).thenReturn(
                new PurchaseOrderSummary("PO-1", "ISSUED", "3D",
                        List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10")))));
        when(repository.findByPoId(eq("PO-1"), any())).thenReturn(List.of(
                new GRNDocument(null, "GRN/26-27/001", "PO-1", "3D", "DN-OLD",
                        Instant.parse("2026-09-21T00:00:00Z"), Instant.parse("2026-09-21T01:00:00Z"),
                        "user-1", com.recnaile.inward.model.GRNStatus.DRAFT,
                        List.of(new com.recnaile.inward.model.GRNLine("ITEM-1", new BigDecimal("10"),
                                new BigDecimal("6"), BigDecimal.ZERO, "EA", null)))
        ));
        when(grnNumberGenerator.next()).thenReturn("GRN/26-27/002");
        when(repository.save(any(GRNDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateGRNRequest request = new CreateGRNRequest(
                "PO-1", "DN-NEW", Instant.parse("2026-09-22T00:00:00Z"), "user-1",
                List.of(new CreateGRNLineRequest("ITEM-1", "EA", new BigDecimal("3"), new BigDecimal("1"), "Damaged")));

        GRNResponse response = service.create(request);

        assertEquals("GRN/26-27/002", response.grnId());
        // The newly-created GRN contains only this receipt (3 accepted + 1 rejected = 4).
        // The previous GRN already received 6, so the cumulative PO receipt is 10.
        assertEquals(new BigDecimal("4"),
                response.items().get(0).acceptedQuantity().add(response.items().get(0).rejectedQuantity()));
        verify(repository).save(any(GRNDocument.class));
    }

    @Test
    void tracksRemainingQuantityAndFullReceiptAcrossGrns() {
        PurchaseOrderSummary po = new PurchaseOrderSummary(
                "PO-1", "ISSUED", "3D",
                List.of(new PurchaseOrderLineSummary("PO-LINE-1", "ITEM-1", new BigDecimal("10"))));

        when(repository.findByPoId(eq("PO-1"), any())).thenReturn(List.of(
                new GRNDocument(null, "GRN/26-27/001", "PO-1", "3D", "DN-1",
                        Instant.parse("2026-09-21T00:00:00Z"), Instant.parse("2026-09-21T01:00:00Z"),
                        "user-1", com.recnaile.inward.model.GRNStatus.DRAFT,
                        List.of(new com.recnaile.inward.model.GRNLine("ITEM-1", new BigDecimal("10"),
                                new BigDecimal("6"), BigDecimal.ZERO, "EA", null)))
        ));

        GRNReceiptProgressService.GRNReceiptProgress progress =
                receiptProgressService.calculateProgress("PO-1", po);

        assertEquals(new BigDecimal("6"), progress.receivedQuantities().get("ITEM-1"));
        assertEquals(new BigDecimal("4"), progress.remainingQuantities().get("ITEM-1"));
        assertFalse(progress.fullyReceived());
    }

}
