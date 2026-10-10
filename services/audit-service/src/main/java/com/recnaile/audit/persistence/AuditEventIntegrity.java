package com.recnaile.audit.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Stable SHA-256 digest over the persisted audit fields, excluding the digest itself. */
public final class AuditEventIntegrity {
    private AuditEventIntegrity() { }

    public static String calculate(AuditEventDocument event) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<String> fields = List.of(
                value(event.eventId()), value(event.eventType()), Integer.toString(event.eventVersion()),
                value(event.occurredAt()), value(event.producer()), value(event.correlationId()), value(event.traceId()),
                value(event.actorId()), String.join("\u001f", event.actorRoles() == null ? List.of() : event.actorRoles()),
                value(event.service()), value(event.entityType()), value(event.entityId()), value(event.action()),
                value(event.timestamp()), value(event.beforeStatus()), value(event.afterStatus()), value(event.poId()),
                value(event.deliveryNoteNumber()), value(event.receivedBy())
            );
            for (String field : fields) {
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static boolean isValid(AuditEventDocument event) {
        return event != null && event.integrityHash() != null
                && MessageDigest.isEqual(event.integrityHash().getBytes(StandardCharsets.US_ASCII),
                        calculate(event).getBytes(StandardCharsets.US_ASCII));
    }

    private static String value(Object value) { return value == null ? "<null>" : value.toString(); }
}
