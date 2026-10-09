package com.recnaile.asset.model;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/** Stable, server-generated identifier for an asset lifecycle. */
public record AssetId(String value) {

    public AssetId {
        Objects.requireNonNull(value, "Asset ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Asset ID must not be blank");
        }
    }

    /**
     * Normalizes raw slash-form and URL-encoded asset IDs to the canonical
     * persisted representation, e.g. ASSET%2F26-27%2F001.
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Asset ID must not be blank");
        }

        String normalized = value.trim();

        // Swagger/proxies can encode an already encoded identifier once more.
        if (normalized.toUpperCase(Locale.ROOT).contains("%25")) {
            try {
                normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                // Continue with the supplied value; validation below still applies.
            }
        }

        normalized = normalized.replace("/", "%2F")
                .replaceAll("(?i)%2f", "%2F");

        return normalized;
    }
}
