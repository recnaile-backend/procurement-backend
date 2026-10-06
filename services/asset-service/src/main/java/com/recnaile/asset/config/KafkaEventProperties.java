package com.recnaile.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaEventProperties(
        boolean enabled,
        String consumerGroup,
        Topics topics) {

    public record Topics(
            String stockIssueApproved,
            String returnAccepted) {
    }
}
