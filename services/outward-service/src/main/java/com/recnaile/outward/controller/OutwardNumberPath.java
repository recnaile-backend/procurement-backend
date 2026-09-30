package com.recnaile.outward.controller;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

/**
 * Normalizes the outward number received from an HTTP path variable.
 *
 * The persisted business identifier is intentionally URL-encoded, e.g.
 * OUT%2F26-27%2F001. Swagger may encode the percent sign again and send
 * OUT%252F26-27%252F001. Decoding exactly one layer converts the latter back
 * to the persisted identifier without converting %2F into a path separator.
 */
public final class OutwardNumberPath {

    private OutwardNumberPath() {
    }

    public static String normalize(String outwardNumber) {
        if (outwardNumber == null || outwardNumber.isBlank()) {
            return outwardNumber;
        }
        if (outwardNumber.contains("%25") || outwardNumber.contains("%25".toLowerCase())) {
            try {
                return URLDecoder.decode(outwardNumber, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                return outwardNumber;
            }
        }
        return outwardNumber;
    }
}
