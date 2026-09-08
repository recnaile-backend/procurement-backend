package com.recnaile.auth.service;

import com.recnaile.auth.config.JwtProperties;
import com.recnaile.auth.dto.LoginRequest;
import com.recnaile.auth.dto.LoginResponse;
import com.recnaile.auth.dto.RefreshTokenRequest;
import com.recnaile.auth.dto.RegisterRequest;
import com.recnaile.auth.dto.RegisterResponse;
import com.recnaile.auth.model.RefreshToken;
import com.recnaile.auth.model.Role;
import com.recnaile.auth.model.User;
import com.recnaile.auth.repository.RefreshTokenRepository;
import com.recnaile.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_DAYS = 30L;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    // =========================================================
    // REGISTER
    // =========================================================

    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                "Email already registered"
            );
        }

        Instant now = Instant.now();

        User user = User.builder()
            .email(normalizedEmail)
            .passwordHash(
                passwordEncoder.encode(request.password())
            )
            .firstName(request.firstName().trim())
            .lastName(request.lastName().trim())
            .departmentId(request.departmentId().trim())
            .roles(Set.of(Role.EMPLOYEE))
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .createdAt(now)
            .updatedAt(now)
            .build();

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getDepartmentId(),
            savedUser.getRoles(),
            savedUser.getEnabled()
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        User user = userRepository
            .findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Invalid email or password"
                )
            );

        if (!user.getEnabled()) {
            throw new IllegalArgumentException(
                "Account is disabled"
            );
        }

        if (user.getAccountLocked()) {
            throw new IllegalArgumentException(
                "Account is locked"
            );
        }

        /*
         * Check password.
         */
        if (!passwordEncoder.matches(
            request.password(),
            user.getPasswordHash()
        )) {

            int failedAttempts =
                user.getFailedLoginAttempts() == null
                    ? 0
                    : user.getFailedLoginAttempts();

            failedAttempts++;

            user.setFailedLoginAttempts(failedAttempts);

            /*
             * Lock account after maximum failed attempts.
             */
            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
            }

            user.setUpdatedAt(Instant.now());

            userRepository.save(user);

            if (user.getAccountLocked()) {
                throw new IllegalArgumentException(
                    "Account is locked"
                );
            }

            throw new IllegalArgumentException(
                "Invalid email or password"
            );
        }

        /*
         * Password is correct.
         * Reset failed login attempts.
         */
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return createTokenResponse(user);
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    public LoginResponse refresh(RefreshTokenRequest request) {

        String refreshToken = request.refreshToken();

        /*
         * Never store the raw refresh token in MongoDB.
         * Hash it before looking it up.
         */
        String tokenHash =
            refreshTokenService.hashToken(refreshToken);

        Instant now = Instant.now();

        /*
         * Find only an active refresh token.
         *
         * Repository query guarantees:
         *
         * - tokenHash matches
         * - revokedAt is null
         * - expiresAt is still in the future
         */
        RefreshToken storedToken =
            refreshTokenRepository
                .findActiveToken(tokenHash, now)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Invalid, expired, or revoked refresh token"
                    )
                );

        /*
         * Load the associated user.
         */
        User user =
            userRepository.findById(storedToken.getUserId())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "User not found"
                    )
                );

        /*
         * Do not issue new tokens for disabled accounts.
         */
        if (!user.getEnabled()) {
            throw new IllegalArgumentException(
                "Account is disabled"
            );
        }

        /*
         * Do not issue new tokens for locked accounts.
         */
        if (user.getAccountLocked()) {
            throw new IllegalArgumentException(
                "Account is locked"
            );
        }

        /*
         * Refresh-token rotation.
         *
         * The old refresh token becomes unusable immediately.
         */
        storedToken.setRevokedAt(now);

        refreshTokenRepository.save(storedToken);

        /*
         * Issue a completely new access token
         * and refresh token.
         */
        return createTokenResponse(user);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    public void logout(RefreshTokenRequest request) {

        String refreshToken = request.refreshToken();

        /*
         * Hash the supplied refresh token before querying MongoDB.
         */
        String tokenHash =
            refreshTokenService.hashToken(refreshToken);

        RefreshToken storedToken =
            refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Invalid refresh token"
                    )
                );

        /*
         * Prevent repeated logout/revocation operations.
         */
        if (storedToken.getRevokedAt() != null) {
            throw new IllegalArgumentException(
                "Refresh token has already been revoked"
            );
        }

        /*
         * Revoke the refresh token.
         */
        storedToken.setRevokedAt(Instant.now());

        refreshTokenRepository.save(storedToken);
    }

    // =========================================================
    // UNLOCK ACCOUNT
    // =========================================================

    public void unlockAccount(String userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "User not found"
                )
            );

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

    // =========================================================
    // CREATE ACCESS + REFRESH TOKENS
    // =========================================================

    private LoginResponse createTokenResponse(User user) {

        Instant now = Instant.now();

        long expiresInSeconds =
            jwtProperties.accessTokenMinutes() * 60;

        Instant expiresAt =
            now.plusSeconds(expiresInSeconds);

        /*
         * Create JWT access token.
         */
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId())
            .claim("email", user.getEmail())
            .claim(
                "roles",
                user.getRoles()
                    .stream()
                    .map(Role::name)
                    .toList()
            )
            .build();

        String accessToken = jwtEncoder
            .encode(
                JwtEncoderParameters.from(claims)
            )
            .getTokenValue();

        /*
         * Generate a cryptographically secure refresh token.
         */
        String refreshToken =
            refreshTokenService.generateToken();

        /*
         * Store ONLY the SHA-256 hash.
         * The raw refresh token is returned only to the client.
         */
        String refreshTokenHash =
            refreshTokenService.hashToken(refreshToken);

        /*
         * Refresh token expires after 30 days.
         */
        RefreshToken refreshTokenEntity =
            RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .userId(user.getId())
                .expiresAt(
                    now.plusSeconds(
                        REFRESH_TOKEN_DAYS * 24 * 60 * 60
                    )
                )
                .createdAt(now)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            expiresInSeconds
        );
    }
}