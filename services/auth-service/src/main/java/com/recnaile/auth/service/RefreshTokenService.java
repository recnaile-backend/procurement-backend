package com.recnaile.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure refresh token.
     */
    public String generateToken() {

        byte[] randomBytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
    }

    /**
     * Hashes the refresh token using SHA-256.
     *
     * Only the hash should be persisted in MongoDB.
     */
    public String hashToken(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                "Refresh token must not be empty"
            );
        }

        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash =
                digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
                );

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                "SHA-256 algorithm not available",
                e
            );
        }
    }
}