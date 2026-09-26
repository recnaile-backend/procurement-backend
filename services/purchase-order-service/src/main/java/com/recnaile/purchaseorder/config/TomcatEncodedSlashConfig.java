package com.recnaile.purchaseorder.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Preserves encoded slashes in PO path identifiers so a stored identifier such
 * as PO%2F26-27%2F001 is received by the application without being converted
 * into path separators.
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
