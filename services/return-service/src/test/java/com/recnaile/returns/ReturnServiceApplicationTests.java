package com.recnaile.returns;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReturnServiceApplicationTests {


    @Test
    void contextLoads() {
        assertThat(ReturnServiceApplication.class).isNotNull();
    }
}
