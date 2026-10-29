package com.rocket.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlMs;

    // @Value reads settings from application.yml
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = ttlMinutes * 60_000;
    }

    public record AccessClaims(Long userId, String sessionId) {}

    public String createAccessToken(Long userId, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId)                              // ← new
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMs))
                .signWith(key)
                .compact();
    }

    public AccessClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AccessClaims(Long.valueOf(claims.getSubject()), claims.get("sid", String.class));
    }

    public long accessTtlSeconds() {
        return accessTtlMs / 1000;
    }
}