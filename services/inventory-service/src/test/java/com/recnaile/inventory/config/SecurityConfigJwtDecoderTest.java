package com.recnaile.inventory.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigJwtDecoderTest {

    @Test
    void createsHs256DecoderWithPlatformSecret() {
        SecurityConfig config = new SecurityConfig();

        Object decoder = config.jwtDecoder(
            "bGDC8C7Dvd4xpFBJnFAp_SvvVMUEC2_EBHGnro00vO0",
            "auth-service"
        );

        assertNotNull(decoder);
    }

    @Test
    void rejectsMissingSecret() {
        SecurityConfig config = new SecurityConfig();

        assertThrows(IllegalStateException.class,
            () -> config.jwtDecoder("", "auth-service"));
    }

    @Test
    void rejectsShortSecret() {
        SecurityConfig config = new SecurityConfig();

        assertThrows(IllegalStateException.class,
            () -> config.jwtDecoder("short-secret", "auth-service"));
    }
}
