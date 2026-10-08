package com.recnaile.asset.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Normalizes platform business references to the persisted URL-encoded form.
 * The service does not generate references owned by other services; it only
 * normalizes references received through APIs/events.
 */
public final class ReferenceNumberNormalizer {

    private ReferenceNumberNormalizer() {
    }

    public static String outwardNumber(String value) {
        return normalize(value, "OUT", "Outward number");
    }

    public static String returnId(String value) {
        return normalize(value, "RET", "Return ID");
    }

    private static String normalize(String value, String prefix, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }

        String normalized = value.trim();
        if (normalized.toUpperCase(Locale.ROOT).contains("%25")) {
            try {
                normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                // Continue with the supplied value.
            }
        }

        normalized = normalized.replace("/", "%2F")
                .replaceAll("(?i)%2f", "%2F");

        if (!normalized.startsWith(prefix + "%2F")) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return normalized;
    }
}
