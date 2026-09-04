package com.procurement.platform.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.procurement.platform.gateway.controller.PlatformController;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformControllerTest {

    @Test
    void pingReturnsGatewayStatus() {
        Map<String, Object> response = new PlatformController().ping();

        assertThat(response).containsEntry("service", "api-gateway");
        assertThat(response).containsEntry("status", "UP");
        assertThat(response).containsKey("timestamp");
    }
}
