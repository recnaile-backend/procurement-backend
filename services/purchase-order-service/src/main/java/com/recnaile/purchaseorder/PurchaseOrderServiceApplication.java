package com.recnaile.purchaseorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PurchaseOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseOrderServiceApplication.class, args);
    }
}
