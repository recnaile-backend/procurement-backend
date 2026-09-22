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
                    text(data, "status", null),
                    text(data, "vendorName", null),
                    text(data, "projectId", null),
                    text(data, "departmentName", null),
                    localDate(data, "expectedDeliveryDate"),
                    items(data.path("items")));
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
                    text(item, "itemId", null),
                    text(item, "itemName", null),
                    decimal(item, "quantity", item.path("requestedQuantity")),
                    decimal(item, "unitPrice", item.path("estimatedUnitPrice")),
                    decimal(item, "gstRate", item.path("gstRate"))));
        }
        return List.copyOf(result);
    }

    private BigDecimal decimal(JsonNode node, String preferred, JsonNode fallback) {
        JsonNode value = node.path(preferred);
        if (value.isMissingNode() || value.isNull()) {
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
                    "Indent-service returned a non-numeric item value");
        }
    }

    private LocalDate localDate(JsonNode node, String field) {
        String value = text(node, field, null);
        return value == null ? null : LocalDate.parse(value);
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.asText();
    }
}
