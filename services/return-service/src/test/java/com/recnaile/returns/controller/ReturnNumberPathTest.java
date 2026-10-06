package com.recnaile.returns.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnNumberPathTest {

    @Test
    void normalizesSwaggerDoubleEncodedReturnNumber() {
        assertEquals("RETURN%2F26-27%2F001",
                ReturnNumberPath.normalize("RETURN%252F26-27%252F001"));
    }

    @Test
    void preservesPersistedEncodedReturnNumber() {
        assertEquals("RETURN%2F26-27%2F001",
                ReturnNumberPath.normalize("RETURN%2F26-27%2F001"));
    }
}
