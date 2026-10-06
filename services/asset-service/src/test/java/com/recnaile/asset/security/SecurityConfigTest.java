package com.recnaile.asset.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityConfigTest {

    @Test
    void jwtRolesAreConvertedToRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("employee-1")
                .claim("roles", List.of("EMPLOYEE", "TL"))
                .build();

        @SuppressWarnings("unchecked")
        var converter = (org.springframework.core.convert.converter.Converter<Jwt, ? extends Authentication>)
                new SecurityConfig().jwtAuthenticationConverter();

        Authentication authentication = converter.convert(jwt);
        assertTrue(authentication instanceof JwtAuthenticationToken);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TL")));
    }
}
