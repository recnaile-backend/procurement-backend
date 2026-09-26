package com.recnaile.inward;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class InwardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InwardServiceApplication.class, args);
    }
}
