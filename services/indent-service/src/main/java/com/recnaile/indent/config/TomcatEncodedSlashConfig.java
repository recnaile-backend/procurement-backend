package com.recnaile.indent.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Preserves the double-encoded percent representation used for the literal
 * indent number.
 *
 * Canonical Mongo/business value:
 *
 *     IN%2F26-27%2F004
 *
 * HTTP path representation:
 *
 *     IN%252F26-27%252F004
 *
 * Tomcat must keep %25 unchanged during its URI processing. Otherwise the
 * first decoding stage can turn %252F into %2F before Spring Security sees
 * the request, which then looks like an encoded slash.
 *
 * We do NOT enable encoded-slash decoding. The slash is part of the literal
 * business identifier and must never become a path separator.
 */
@Configuration
public class TomcatEncodedSlashConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> encodedSlashCustomizer() {

        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setEncodedSolidusHandling("passthrough")
        );
    }
}
