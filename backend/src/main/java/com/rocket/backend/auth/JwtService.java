package com.rocket.backend.auth;

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

    public String createAccessToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())                          // who
                .issuedAt(now)                                       // when made
                .expiration(new Date(now.getTime() + accessTtlMs))   // when it dies
                .signWith(key)                                       // the signature
                .compact();                                          // → "xxx.yyy.zzz"
    }

    // Throws an exception if the token is fake, changed or expired
    public Long parseUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }

    public long accessTtlSeconds() {
        return accessTtlMs / 1000;
    }
}