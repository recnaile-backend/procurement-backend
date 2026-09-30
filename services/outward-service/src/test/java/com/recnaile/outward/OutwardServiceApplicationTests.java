package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.recnaile.outward.outbox.OutboxEventRepository;
import com.recnaile.outward.repository.OutwardIssueTransactionRepository;
import com.recnaile.outward.repository.OutwardRequestRepository;
import com.recnaile.outward.repository.OutwardSequenceRepository;

@SpringBootTest(properties = {
        "MONGODB_BASE_URI=mongodb://localhost:27017",
        "MONGODB_DATABASE=outward_test",
        "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
        "OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317",
        "JWT_ISSUER=auth-service",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
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

    @TestConfiguration(proxyBeanMethods = false)
    static class InfrastructureMocks {

        @Bean
        MongoDatabaseFactory mongoDatabaseFactory() {
            return mock(MongoDatabaseFactory.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return mock(MongoTemplate.class);
        }

        @Bean
        OutwardRequestRepository outwardRequestRepository() {
            return mock(OutwardRequestRepository.class);
        }

        @Bean
        OutwardIssueTransactionRepository outwardIssueTransactionRepository() {
            return mock(OutwardIssueTransactionRepository.class);
        }

        @Bean
        OutwardSequenceRepository outwardSequenceRepository() {
            return mock(OutwardSequenceRepository.class);
        }

        @Bean
        OutboxEventRepository outboxEventRepository() {
            return mock(OutboxEventRepository.class);
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }

    @Test
    void contextLoads() {
        assertNotNull(testJwtSecret());
    }
}
