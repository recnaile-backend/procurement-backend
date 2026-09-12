package com.recnaile.masterdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MasterDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                MasterDataServiceApplication.class,
                args
        );
    }
}