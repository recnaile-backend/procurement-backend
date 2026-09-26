package com.recnaile.inward.dto;

import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record GRNResponse(
        String id,
        @Schema(description = "Backend-generated business identifier for the Goods Receipt Note", example = "GRN/26-27/001", accessMode = Schema.AccessMode.READ_ONLY)
        String grnId,
        String poId,
        String vendorName,
        String deliveryNoteNumber,
        Instant deliveryDate,
        Instant receivedAt,
        String receivedBy,
        GRNStatus grnStatus,
        List<GRNLine> items,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static GRNResponse from(GRNDocument grn) {
        return new GRNResponse(
                grn.getId(),
                grn.getGrnId(),
                grn.getPoId(),
                grn.getVendorName(),
                grn.getDeliveryNoteNumber(),
                grn.getDeliveryDate(),
                grn.getReceivedAt(),
                grn.getReceivedBy(),
                grn.getGrnStatus(),
                grn.getLines(),
                grn.getVersion(),
                grn.getCreatedAt(),
                grn.getUpdatedAt()
        );
    }
}
