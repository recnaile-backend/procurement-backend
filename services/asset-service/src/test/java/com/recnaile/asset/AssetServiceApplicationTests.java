package com.recnaile.asset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/asset-test",
        "management.endpoints.web.exposure.include=health,info",
        "app.kafka.enabled=false"
})
class AssetServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
