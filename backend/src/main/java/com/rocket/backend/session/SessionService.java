package com.rocket.backend.session;

import com.rocket.backend.common.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserSessionRepository sessionRepository;

    @Value("${jwt.refresh-ttl-days}")
    private long refreshTtlDays;    // not final, so Lombok's constructor skips it; Spring fills it in

    // LOGIN: kick out any old session, start a new one
    @Transactional
    public SessionTokens start(Long userId) {
        sessionRepository.deleteByUserId(userId);            // ← this line kicks the old device

        String refreshToken = newRefreshToken();
        UserSession s = new UserSession();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setRefreshHash(sha256(refreshToken));              // store the HASH only
        s.setExpiresAt(LocalDateTime.now().plusDays(refreshTtlDays));
        sessionRepository.save(s);

        return new SessionTokens(userId, s.getId(), refreshToken);  // plain token goes to the client once
    }

    // REFRESH: swap the old refresh token for a new one (rotation)
    @Transactional
    public SessionTokens rotate(String refreshToken) {
        UserSession s = sessionRepository.findByRefreshHash(sha256(refreshToken))
                .orElseThrow(() -> new GameException(
                        HttpStatus.UNAUTHORIZED, "REFRESH_INVALID", "Please log in again"));

        if (s.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GameException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRED", "Please log in again");
        }

        String newToken = newRefreshToken();
        s.setRefreshHash(sha256(newToken));
        s.setExpiresAt(LocalDateTime.now().plusDays(refreshTtlDays));
        // No save() needed! Inside @Transactional, JPA notices the changed fields
        // and writes an UPDATE at commit. This is called "dirty checking".

        return new SessionTokens(s.getUserId(), s.getId(), newToken);
    }

    // Called by the filter on EVERY request
    public boolean isCurrent(Long userId, String sessionId) {
        return sessionRepository.findByUserId(userId)
                .map(s -> s.getId().equals(sessionId))
                .orElse(false);
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByRefreshHash(sha256(refreshToken))
                .ifPresent(sessionRepository::delete);
    }

    // 32 random bytes → a URL-safe string like "kq3X9_aZ..."
    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);   // 64 hex characters
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);       // can't happen: every JVM has SHA-256
        }
    }
}