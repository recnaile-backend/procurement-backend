package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssetIdTest {

    @Test
    void normalizesRawSlashFormToCanonicalEncodedForm() {
        assertEquals("ASSET%2F26-27%2F001", AssetId.normalize("ASSET/26-27/001"));
    }

    @Test
    void keepsCanonicalEncodedForm() {
        assertEquals("ASSET%2F26-27%2F001", AssetId.normalize("ASSET%2F26-27%2F001"));
    }

    @Test
    void normalizesDoubleEncodedFormFromSwagger() {
        assertEquals("ASSET%2F26-27%2F001", AssetId.normalize("ASSET%252F26-27%252F001"));
    }

}
