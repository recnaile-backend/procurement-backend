package com.recnaile.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.recnaile.auth.model.RefreshToken;

public interface RefreshTokenRepository
    extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds only a currently active refresh token.
     *
     * Active means:
     * - token hash matches
     * - token has not been revoked
     * - token has not expired
     */
    @Query("""
        {
            'tokenHash': ?0,
            'revokedAt': null,
            'expiresAt': { $gt: ?1 }
        }
        """)
    Optional<RefreshToken> findActiveToken(
        String tokenHash,
        Instant now
    );

    /**
     * Used when only the MongoDB ID is required.
     */
    @Query(
        value = """
            {
                'tokenHash': ?0,
                'revokedAt': null,
                'expiresAt': { $gt: ?1 }
            }
            """,
        fields = "{ '_id': 1 }"
    )
    Optional<RefreshToken> findActiveTokenId(
        String tokenHash,
        Instant now
    );

    /**
     * Deletes all refresh tokens belonging to a user.
     */
    void deleteByUserId(String userId);

    /**
     * Deletes expired refresh tokens.
     */
    long deleteByExpiresAtBefore(Instant now);

    /**
     * Deletes revoked refresh tokens older than the retention period.
     */
    long deleteByRevokedAtBefore(Instant cutoff);
}