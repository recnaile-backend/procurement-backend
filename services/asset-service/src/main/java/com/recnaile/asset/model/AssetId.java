package com.recnaile.asset.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identifier for an asset lifecycle.
 *
 * <p>The persisted Asset ID intentionally contains the URL-encoded separator
 * sequence required by the project contract, for example
 * {@code ASSET%2F26-27%2F001}.</p>
 */
public record AssetId(String value) {

    private static final Pattern FORMAT =
            Pattern.compile("ASSET%2F\\d{2}-\\d{2}%2F\\d{3,}");

    public AssetId {
        Objects.requireNonNull(value, "Asset ID must not be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Asset ID must match ASSET%2FYY-YY%2FNNN format");
        }
    }

    public static AssetId of(String value) {
        return new AssetId(value);
    }
}
