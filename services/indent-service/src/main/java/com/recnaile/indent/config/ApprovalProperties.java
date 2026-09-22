package com.recnaile.indent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.approval")
public class ApprovalProperties {

    /**
     * Requests strictly below this amount route to TL.
     * Requests at or above this amount route to MD.
     */
    private BigDecimal threshold;

    /**
     * SLA duration starts when an indent becomes pending approval.
     */
    private Duration slaDuration = Duration.ofHours(24);

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public Duration getSlaDuration() {
        return slaDuration;
    }

    public void setSlaDuration(Duration slaDuration) {
        this.slaDuration = slaDuration;
    }
}
