package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.recnaile.outward.controller.OutwardNumberPath;
import org.junit.jupiter.api.Test;

class OutwardNumberPathTest {

    @Test
    void normalizesSwaggerDoubleEncodedOutwardNumber() {
        assertEquals("OUT%2F26-27%2F001",
                OutwardNumberPath.normalize("OUT%252F26-27%252F001"));
    }

    @Test
    void leavesPersistedEncodedOutwardNumberUnchanged() {
        assertEquals("OUT%2F26-27%2F001",
                OutwardNumberPath.normalize("OUT%2F26-27%2F001"));
    }
}
