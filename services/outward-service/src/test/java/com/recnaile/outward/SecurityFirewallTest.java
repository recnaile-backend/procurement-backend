package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recnaile.outward.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

class SecurityFirewallTest {

    @Test
    void configuresFirewallForEncodedOutwardNumbers() {
        HttpFirewall firewall = new SecurityConfig().outwardHttpFirewall();
        assertNotNull(firewall);
        assertTrue(firewall instanceof StrictHttpFirewall);
    }
}
