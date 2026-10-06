package com.recnaile.asset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/asset-test",
        "management.endpoints.web.exposure.include=health,info",
        "app.kafka.enabled=false"
})
class AssetServiceApplicationTests {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
