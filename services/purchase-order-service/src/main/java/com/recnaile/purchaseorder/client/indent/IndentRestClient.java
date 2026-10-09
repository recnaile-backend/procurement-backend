package com.recnaile.purchaseorder.client.indent;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;
import com.recnaile.purchaseorder.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class IndentRestClient implements IndentClient {

    private final RestClient restClient;

    public IndentRestClient(@Qualifier("indentHttpRestClient") RestClient indentRestClient) {
        this.restClient = indentRestClient;
    }

    @Override
    public IndentDetails getIndent(String indentNumber) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/indents/{indentNumber}")
                            .build(indentNumber))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException(
                                    "INDENT_NOT_FOUND",
                                    "Indent not found in indent-service: " + indentNumber);
                        }
                        throw new MasterDataIntegrationException(
                                "INDENT_SERVICE_CLIENT_ERROR",
                                "Indent-service rejected the indent lookup with HTTP "
                                        + response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) ->
                            new MasterDataIntegrationException(
                                    "INDENT_SERVICE_UNAVAILABLE",
                                    "Indent-service is unavailable while resolving indent: " + indentNumber))
                    .body(JsonNode.class);

            JsonNode data = root == null ? null : root.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                throw new MasterDataIntegrationException(
                        "INDENT_SERVICE_INVALID_RESPONSE",
                        "Indent-service returned no data for: " + indentNumber);
            }

            return new IndentDetails(
                    text(data, "indentNumber", indentNumber),
                    firstText(data, "requesterId", "requestBy", "requestId"),
                    text(data, "departmentId", null),
                    text(data, "departmentName", null),
                    text(data, "projectId", null),
                    text(data, "reason", null),
                    items(data.path("items")),
                    decimal(data, "estimatedTotalAmount", data.path("totalAmount")),
                    text(data, "status", null),
                    text(data, "approvalLevel", null),
                    text(data, "verifierId", null),
                    instant(data, "verifiedAt"),
                    text(data, "verificationRemarks", null),
                    text(data, "approverId", null),
                    instant(data, "approvedAt"),
                    text(data, "approvalRemarks", null));
        } catch (ResourceNotFoundException | MasterDataIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataIntegrationException(
                    "INDENT_SERVICE_UNAVAILABLE",
                    "Unable to reach indent-service while resolving indent: " + indentNumber,
                    ex);
        }
    }

    private List<IndentDetails.Item> items(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<IndentDetails.Item> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(new IndentDetails.Item(
                    text(item, "itemName", null),
                    decimal(item, "quantity", item.path("requestedQuantity")),
                    decimal(item, "unitPrice", item.path("estimatedUnitPrice")),
                    decimal(item, "gstRate", null),
                    decimal(item, "estimatedLineAmount", null),
                    text(item, "remarks", null)));
        }
        return List.copyOf(result);
    }

    private BigDecimal decimal(JsonNode node, String preferred, JsonNode fallback) {
        JsonNode value = node == null ? null : node.path(preferred);
        if (value == null || value.isMissingNode() || value.isNull()) {
            value = fallback;
        }
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
        } catch (NumberFormatException ex) {
            throw new MasterDataIntegrationException(
                    "INDENT_SERVICE_INVALID_RESPONSE",
                    "Indent-service returned a non-numeric value for " + preferred);
        }
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field, null);
        return value == null ? null : Instant.parse(value);
    }

    private String firstText(JsonNode node, String primary, String... fallbacks) {
        String value = text(node, primary, null);
        if (value != null) {
            return value;
        }
        for (String fallback : fallbacks) {
            value = text(node, fallback, null);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.asText();
    }
}
