package com.recnaile.returns.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class ReturnNumberPath {
    private ReturnNumberPath() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String normalized = value.trim();
        if (normalized.contains("%25")) {
            try {
                normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                // Let repository lookup return not-found rather than decoding malformed input twice.
            }
        }

        // Backward compatibility for the pre-final RET prefix. Persisted/current
        // business return numbers always use RETURN%2F... .
        if (normalized.startsWith("RET%2F")) {
            normalized = "RETURN%2F" + normalized.substring("RET%2F".length());
        }
        return normalized;
    }
}
