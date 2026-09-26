package com.recnaile.outward.domain;

import com.recnaile.outward.domain.number.OutwardNumberGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutwardNumberGeneratorTest {

    @Test
    void generatesEncodedNumber() {
        assertEquals("OUT%2F26-27%2F001", OutwardNumberGenerator.generate(2026, 1));
        assertEquals("OUT%2F26-27%2F999", OutwardNumberGenerator.generate(2026, 999));
    }

    @Test
    void rejectsInvalidFiscalYear() {
        assertThrows(IllegalArgumentException.class, () -> OutwardNumberGenerator.generate(1999, 1));
    }

    @Test
    void rejectsInvalidSequence() {
        assertThrows(IllegalArgumentException.class, () -> OutwardNumberGenerator.generate(2026, 0));
    }
}
