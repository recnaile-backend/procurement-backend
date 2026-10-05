package com.recnaile.returns;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReturnServiceApplicationTests {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        assertThat(ReturnServiceApplication.class).isNotNull();
    }
}
