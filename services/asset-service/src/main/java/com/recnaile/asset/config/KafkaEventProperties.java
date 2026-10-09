package com.recnaile.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaEventProperties(
        boolean enabled,
        String consumerGroup,
        Topics topics) {

    /**
     * Keep this as a single canonical constructor so Spring Boot can reliably
     * constructor-bind the nested topic properties. Multiple constructors on
     * the nested record can cause the binder to leave {@code topics} null.
     */
    public record Topics(
            String stockIssueApproved,
            String returnAccepted,
            String assetAssigned,
            String assetReturned,
            String assetStatusChanged,
            String auditEvent,
            String outwardIssued,
            String returnRecorded) {
    }
}
