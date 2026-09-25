package com.recnaile.purchaseorder.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Preserve encoded solidus values inside the PO path variable.
 *
 * A stored PO ID such as PO%2F26-27%2F001 is intentionally treated as one
 * business identifier rather than being split into multiple URL segments.
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
