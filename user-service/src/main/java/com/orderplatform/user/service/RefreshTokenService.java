package com.orderplatform.user.service;

import com.orderplatform.user.config.RefreshTokenProperties;
import com.orderplatform.user.entity.RefreshToken;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.InvalidRefreshTokenException;
import com.orderplatform.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and rotates refresh tokens. Each token works once: using it revokes it and issues a new one,
 * so a revoked token showing up again means it was copied, and all of that user's sessions are ended.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;

    /** Returns the raw token for the client; only its hash is saved. */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshToken(user, hash(token), Instant.now().plus(properties.expiration())));
        return token;
    }

    /** Revokes the token and returns its user; the caller issues the new pair. */
    // noRollbackFor: ending all sessions on reuse must be saved even though the request fails
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public User rotate(String token) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (stored.isRevoked()) {
            Long userId = stored.getUser().getId();
            log.warn("Refresh token reused for user {}; ending all sessions", userId);
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new InvalidRefreshTokenException();
        }
        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        stored.revoke(now);
        return stored.getUser();
    }

    /** Daily at 03:00. Revoked tokens are kept until they expire so that reuse can still be detected. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    // The token is 256 random bits, so a fast hash is enough (BCrypt is for guessable passwords)
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
