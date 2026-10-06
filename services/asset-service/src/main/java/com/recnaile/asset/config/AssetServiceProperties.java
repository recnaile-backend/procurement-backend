package com.recnaile.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AssetServiceProperties(
        String serviceName,
        String description,
        String databaseName) {
}
