package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssetIdTest {

    @Test
    void acceptsRequiredEncodedFormat() {
        AssetId id = AssetId.of("ASSET%2F26-27%2F001");
        assertEquals("ASSET%2F26-27%2F001", id.value());
    }

    @Test
    void acceptsHigherSequenceNumbers() {
        assertDoesNotThrow(() -> AssetId.of("ASSET%2F26-27%2F1000"));
    }

    @Test
    void rejectsDecodedSlashFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> AssetId.of("ASSET/26-27/001"));
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(NullPointerException.class, () -> AssetId.of(null));
        assertThrows(IllegalArgumentException.class, () -> AssetId.of(""));
    }
}
