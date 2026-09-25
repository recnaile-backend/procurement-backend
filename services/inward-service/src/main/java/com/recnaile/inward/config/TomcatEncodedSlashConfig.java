package com.recnaile.inward.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Allows URL-encoded slash separators in GRN IDs to remain part of the
 * path variable while the stored/business identifier remains literal,
 * for example GRN/26-27/001.
 *
 * The URL form is GRN%2F26-27%2F001.
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
