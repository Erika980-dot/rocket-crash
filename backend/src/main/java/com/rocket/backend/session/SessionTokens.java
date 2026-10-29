package com.rocket.backend.session;

public record SessionTokens(Long userId, String sessionId, String refreshToken) {}