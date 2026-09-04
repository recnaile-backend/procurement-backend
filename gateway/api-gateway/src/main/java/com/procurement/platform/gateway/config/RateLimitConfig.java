package com.procurement.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    KeyResolver clientKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(principal -> "user:" + principal.getName())
            .switchIfEmpty(Mono.fromSupplier(() -> {
                var address = exchange.getRequest().getRemoteAddress();
                return "ip:" + (address == null ? "unknown" : address.getAddress().getHostAddress());
            }));
    }
}
