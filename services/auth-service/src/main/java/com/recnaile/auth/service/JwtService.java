package com.recnaile.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.recnaile.auth.config.JwtProperties;
import com.recnaile.auth.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    /**
     * Generates an HS256 access token for the supplied user.
     */
    public String generateAccessToken(User user) {

        Instant issuedAt = Instant.now();

        Instant expiresAt =
            issuedAt.plus(
                jwtProperties.accessTokenMinutes(),
                ChronoUnit.MINUTES
            );

        List<String> roles =
            user.getRoles()
                .stream()
                .map(Enum::name)
                .toList();

        JwtClaimsSet claims =
            JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())

                /*
                 * Keep MongoDB user ID as JWT subject.
                 *
                 * Authentication.getName() currently uses
                 * this value for authenticated-user lookup.
                 */
                .subject(user.getId())

                .issuedAt(issuedAt)
                .expiresAt(expiresAt)

                /*
                 * Employee ID is now the user's login/business identity.
                 */
                .claim("employeeId", user.getEmployeeId())

                /*
                 * Roles are included for authorization.
                 */
                .claim("roles", roles)

                .build();

        JwsHeader header =
            JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    header,
                    claims
                )
            )
            .getTokenValue();
    }

    /**
     * Returns access-token lifetime in seconds.
     */
    public long getAccessTokenExpirationSeconds() {

        return jwtProperties
            .accessTokenMinutes()
            * 60;
    }
}