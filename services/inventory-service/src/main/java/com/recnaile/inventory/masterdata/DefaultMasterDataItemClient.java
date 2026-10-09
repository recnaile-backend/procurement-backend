package com.recnaile.inventory.masterdata;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Read-only integration with Master Data. Inventory never writes item master
 * data and never reads the Master Data MongoDB directly.
 */
@Service
public class DefaultMasterDataItemClient implements MasterDataItemClient {
    private static final long CACHE_SECONDS = 30;

    private final RestClient client;
    private final String jwtSecret;
    private final String issuer;
    private final AtomicReference<Cache> cache = new AtomicReference<>(new Cache(List.of(), Instant.EPOCH));

    public DefaultMasterDataItemClient(
            @Value("${app.master-data.url:http://master-data-service:8082}") String baseUrl,
            @Value("${app.security.jwt.secret-key:}") String jwtSecret,
            @Value("${app.security.jwt.issuer-uri:auth-service}") String issuer) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.jwtSecret = jwtSecret;
        this.issuer = issuer;
    }

    @Override
    public Optional<MasterDataItemResponse> findById(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();
        return findAll().stream()
                .filter(item -> itemId.equals(item.itemId()))
                .findFirst();
    }

    @Override
    public List<MasterDataItemResponse> findAll() {
        Cache current = cache.get();
        if (current.expiresAt().isAfter(Instant.now())) return current.items();

        MasterDataItemResponse[] response;
        try {
            response = client.get()
                    .uri("/api/v1/master-data/items")
                    .headers(headers -> headers.setBearerAuth(serviceToken()))
                    .retrieve()
                    .body(MasterDataItemResponse[].class);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new IllegalStateException("Master Data item list lookup failed", ex);
        }
        List<MasterDataItemResponse> items = response == null ? List.of() : java.util.Arrays.asList(response);
        cache.set(new Cache(items, Instant.now().plusSeconds(CACHE_SECONDS)));
        return items;
    }

    @Override
    public Optional<MasterDataItemResponse> findUniqueByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        List<MasterDataItemResponse> matches = findAll().stream()
                .filter(item -> item.name() != null && item.name().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Master Data contains multiple items with name: " + name);
        }
        return matches.stream().findFirst();
    }

    private String serviceToken() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_KEY must be configured for Master Data integration");
        }
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET_KEY must be at least 256 bits (32 bytes)");
        }
        try {
            Instant now = Instant.now();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject("inventory-service")
                            .claim("roles", List.of("INVENTORY_SERVICE"))
                            .issueTime(java.util.Date.from(now))
                            .expirationTime(java.util.Date.from(now.plusSeconds(60)))
                            .build());
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create service token for Master Data integration", ex);
        }
    }

    private record Cache(List<MasterDataItemResponse> items, Instant expiresAt) { }
}
