package com.recnaile.asset.model;

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
     * Normalizes an HTTP-decoded Asset ID to the canonical stored representation.
     * The persisted value intentionally contains the literal %2F sequence.
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Asset ID must not be blank");
        }
        return value.replace("/", "%2F");
    }
}
