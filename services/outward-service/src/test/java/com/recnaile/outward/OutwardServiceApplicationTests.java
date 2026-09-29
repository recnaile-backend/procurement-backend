package com.recnaile.outward;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "MONGODB_BASE_URI=mongodb://localhost:27017",
        "MONGODB_DATABASE=outward_test",
        "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
        "OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317",
        "JWT_ISSUER=auth-service",
        "JWT_SECRET_KEY=test-secret-key-for-outward-service-32-bytes-minimum"
})
class OutwardServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
