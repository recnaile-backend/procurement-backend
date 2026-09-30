package com.recnaile.returns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReturnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReturnServiceApplication.class, args);
    }
}
