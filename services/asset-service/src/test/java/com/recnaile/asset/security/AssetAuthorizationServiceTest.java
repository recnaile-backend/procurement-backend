package com.recnaile.asset.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;

class AssetAuthorizationServiceTest {

    private final AssetAuthorizationService authorization = new AssetAuthorizationService();

    @Test
    void employeeCanReadOwnAssignedAsset() {
        Asset asset = asset("employee-1", "Engineering");
        Authentication authentication = jwtAuthentication("employee-1", List.of("EMPLOYEE"), "Engineering");
        assertDoesNotThrow(() -> authorization.requireAssetRead(authentication, asset));
    }

    @Test
    void employeeCannotReadAnotherEmployeesAsset() {
        Asset asset = asset("employee-2", "Engineering");
        Authentication authentication = jwtAuthentication("employee-1", List.of("EMPLOYEE"), "Engineering");
        assertThrows(AccessDeniedException.class, () -> authorization.requireAssetRead(authentication, asset));
    }

    @Test
    void teamLeadCanReadAssetsInOwnDepartment() {
        Asset asset = asset("employee-2", "Engineering");
        Authentication authentication = jwtAuthentication("tl-1", List.of("TL"), "Engineering");
        assertDoesNotThrow(() -> authorization.requireAssetRead(authentication, asset));
    }

    @Test
    void procurementCanMutateAnyAsset() {
        Asset asset = asset("employee-2", "Engineering");
        Authentication authentication = jwtAuthentication("proc-1", List.of("PROCUREMENT"), "Finance");
        assertDoesNotThrow(() -> authorization.requireAssetMutation(authentication, asset));
    }

    @Test
    void employeeCannotMutateAsset() {
        Asset asset = asset("employee-1", "Engineering");
        Authentication authentication = jwtAuthentication("employee-1", List.of("EMPLOYEE"), "Engineering");
        assertThrows(AccessDeniedException.class, () -> authorization.requireAssetMutation(authentication, asset));
    }

    private static Asset asset(String employeeId, String department) {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"), "ITEM-1", "OUT-1", "Laptop", "Brand", "Model",
                "issuer-1", AssetStatus.RETURNED, AssetCondition.GOOD, null,
                java.time.Instant.parse("2026-01-01T00:00:00Z"));
        asset.beginNewLifecycle(employeeId, department, java.time.Instant.parse("2026-01-01T00:00:00Z"));
        return asset;
    }

    private static Authentication jwtAuthentication(String subject, List<String> roles, String department) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("roles", roles)
                .claim("departmentName", department)
                .build();
        return new JwtAuthenticationToken(jwt, roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
    }
}
