package com.recnaile.asset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.recnaile.asset.config.KafkaEventProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/asset-test",
        "management.endpoints.web.exposure.include=health,info",
        "app.kafka.enabled=false",
        "app.mongodb.ensure-asset-indexes=false"
})
class AssetServiceApplicationTests {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    KafkaEventProperties kafkaEventProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void kafkaTopicsAreBound() {
        org.junit.jupiter.api.Assertions.assertNotNull(kafkaEventProperties.topics());
        org.junit.jupiter.api.Assertions.assertEquals("audit.events", kafkaEventProperties.topics().auditEvent());
        org.junit.jupiter.api.Assertions.assertEquals("outward.events", kafkaEventProperties.topics().outwardIssued());
        org.junit.jupiter.api.Assertions.assertEquals("return.recorded", kafkaEventProperties.topics().returnRecorded());
    }
}
