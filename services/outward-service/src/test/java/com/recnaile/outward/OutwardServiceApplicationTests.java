package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "MONGODB_BASE_URI=mongodb://localhost:27017",
        "MONGODB_DATABASE=outward_test",
        "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
        "OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317",
        "JWT_ISSUER=auth-service"
})
class OutwardServiceApplicationTests {

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET_KEY", OutwardServiceApplicationTests::testJwtSecret);
    }

    private static String testJwtSecret() {
        byte[] testMaterial = "outward-service-test-key-material-2026"
                .repeat(2)
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(testMaterial);
    }

    @Test
    void contextLoads() {
        assertNotNull(testJwtSecret());
    }
}
