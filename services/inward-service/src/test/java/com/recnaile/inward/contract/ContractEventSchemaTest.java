package com.recnaile.inward.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.model.OutboxEvent;
import com.recnaile.inward.outbox.OutboxService;
import com.recnaile.inward.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContractEventSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void goodsReceivedEventConformsToContractSchema() throws Exception {
        OutboxEvent event = generateGoodsReceivedEvent();
        JsonNode actual = objectMapper.readTree(event.getPayload());

        validateAgainstContract("GOODS_RECEIVED.schema.json", actual);

        assertEquals("GOODS_RECEIVED", actual.path("eventType").asText());
        assertEquals("inward-service", actual.path("producer").asText());
        assertEquals("GRN/26-27/001", actual.path("payload").path("grnId").asText());
        assertEquals("PO-009", actual.path("payload").path("poId").asText());
        assertEquals("ITEM-1", actual.path("payload").path("itemId").asText());
        assertTrue(actual.path("payload").has("acceptedQuantity"));
        assertFalse(actual.path("payload").has("poLineId"),
                "poLineId must not be introduced into the GOODS_RECEIVED contract");
    }

    @Test
    void auditEventConformsToContractSchema() throws Exception {
        OutboxEvent event = generateAuditEvent();
        JsonNode actual = objectMapper.readTree(event.getPayload());

        validateAgainstContract("AUDIT_EVENT.schema.json", actual);

        JsonNode payload = actual.path("payload");
        assertEquals("AUDIT_EVENT", actual.path("eventType").asText());
        assertEquals("inward-service", actual.path("producer").asText());
        assertEquals("GRN", payload.path("entityType").asText());
        assertEquals("GRN/26-27/001", payload.path("entityId").asText());
        assertEquals("GRN_CREATED", payload.path("action").asText());
        assertTrue(payload.path("actorRoles").isArray());
    }

    private OutboxEvent generateGoodsReceivedEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxService service = new OutboxService(repository, objectMapper, clock, "procurement.events");
        service.enqueueGoodsReceived(sampleGrn());

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private OutboxEvent generateAuditEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxService service = new OutboxService(repository, objectMapper, clock, "procurement.events");
        service.enqueueGRNCreateAuditEvent(sampleGrn());

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private GRNResponse sampleGrn() {
        return new GRNResponse(
                "mongo-id", "GRN/26-27/001", "PO-009", "3D", "DN-001",
                Instant.parse("2026-09-24T09:00:00Z"),
                Instant.parse("2026-09-24T10:00:00Z"), "Rajesh", GRNStatus.DRAFT,
                List.of(new GRNLine(
                        "ITEM-1", new BigDecimal("10"), new BigDecimal("9"),
                        BigDecimal.ONE, "PCS", "Damaged")),
                0L, null, null);
    }

    private void validateAgainstContract(String fileName, JsonNode actual) throws Exception {
        JsonNode schemaNode = objectMapper.readTree(
                Files.readString(contractPath(fileName)));
        JsonNode envelopeNode = objectMapper.readTree(
                Files.readString(contractPath("_envelope.schema.json")));
        resolveLocalReferences(schemaNode, envelopeNode);

        JsonSchema schema = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(actual);

        assertTrue(errors.isEmpty(), () -> "Event does not conform to " + fileName + ": " + errors);
    }

    private void resolveLocalReferences(JsonNode node, JsonNode envelopeNode) {
        if (node.isObject()) {
            JsonNode ref = node.get("$ref");
            if (ref != null && "_envelope.schema.json".equals(ref.asText())) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("$ref");
                envelopeNode.fields().forEachRemaining(entry ->
                        ((com.fasterxml.jackson.databind.node.ObjectNode) node).set(entry.getKey(), entry.getValue().deepCopy()));
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                resolveLocalReferences(fields.next().getValue(), envelopeNode);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                resolveLocalReferences(child, envelopeNode);
            }
        }
    }

    private Path contractPath(String fileName) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("contracts/events").resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Could not locate contracts/events/" + fileName
                + " from " + System.getProperty("user.dir"));
    }
}
