package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReferenceNumberNormalizerTest {

    @Test
    void normalizesOutwardNumber() {
        assertEquals("OUT%2F26-27%2F001", ReferenceNumberNormalizer.outwardNumber("OUT/26-27/001"));
        assertEquals("OUT%2F26-27%2F001", ReferenceNumberNormalizer.outwardNumber("OUT%2F26-27%2F001"));
    }

    @Test
    void normalizesReturnId() {
        assertEquals("RET%2F26-27%2F001", ReferenceNumberNormalizer.returnId("RET%2F26-27%2F001"));
        assertEquals("RET%2F26-27%2F001", ReferenceNumberNormalizer.returnId("RET/26-27/001"));
    }

    @Test
    void rejectsWrongReferencePrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceNumberNormalizer.outwardNumber("RET%2F26-27%2F001"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceNumberNormalizer.returnId("OUT%2F26-27%2F001"));
    }
}
