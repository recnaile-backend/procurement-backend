package com.recnaile.inward.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Preserves encoded slash separators in GRN path identifiers so an identifier
 * such as GRN%2F26-27%2F001 is received by the application as one path variable.
 * This matches the Purchase Order Service convention (PO%2F26-27%2F001).
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
