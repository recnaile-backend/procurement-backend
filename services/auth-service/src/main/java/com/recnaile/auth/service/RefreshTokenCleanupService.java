package com.recnaile.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.recnaile.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private static final long REVOKED_TOKEN_RETENTION_DAYS = 7L;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Deletes expired refresh tokens.
     *
     * MongoDB TTL remains the primary cleanup mechanism.
     * This scheduled job provides an additional cleanup layer.
     */
    @Scheduled(
        fixedDelayString =
            "${app.refresh-token.cleanup-interval-ms:3600000}"
    )
    public void cleanupExpiredTokens() {

        Instant now = Instant.now();

        long deleted =
            refreshTokenRepository
                .deleteByExpiresAtBefore(now);

        if (deleted > 0) {
            log.info(
                "Refresh token cleanup: deleted {} expired token(s)",
                deleted
            );
        }
    }

    /**
     * Deletes revoked refresh tokens after the retention period.
     *
     * Revoked tokens are intentionally retained for a limited period
     * before permanent deletion.
     */
    @Scheduled(
        fixedDelayString =
            "${app.refresh-token.cleanup-interval-ms:3600000}"
    )
    public void cleanupRevokedTokens() {

        Instant cutoff =
            Instant.now()
                .minus(
                    REVOKED_TOKEN_RETENTION_DAYS,
                    ChronoUnit.DAYS
                );

        long deleted =
            refreshTokenRepository
                .deleteByRevokedAtBefore(cutoff);

        if (deleted > 0) {
            log.info(
                "Refresh token cleanup: deleted {} revoked token(s)",
                deleted
            );
        }
    }
}